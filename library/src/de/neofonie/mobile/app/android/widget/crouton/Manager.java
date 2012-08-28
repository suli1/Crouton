/*
 * Copyright 2012 Neofonie Mobile GmbH
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package de.neofonie.mobile.app.android.widget.crouton;


import java.util.LinkedList;
import java.util.Queue;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


/**
 * Manager <br>
 * <br>
 * <p/>
 * Manages the lifecycle of {@link Crouton}s.
 */
final class Manager extends Handler {
	private static final class Messages {
		private Messages() {
		}

		public static final int DISPLAY_CROUTON = 0xc2007;
		public static final int ADD_CROUTON_TO_VIEW = 0xc20074dd;
		public static final int REMOVE_CROUTON = 0xc2007de1;
	}

	private static Manager INSTANCE;

	private Queue<Crouton> croutonQueue;

	private Animation inAnimation;
	private Animation outAnimation;

	private Manager() {
		croutonQueue = new LinkedList<Crouton>();
	}

	/**
	 * @return The currently used instance of the {@link Manager}.
	 */
	static synchronized Manager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Manager();
		}

		return INSTANCE;
	}

	/**
	 * Inserts a {@link Crouton} to be displayed.
	 *
	 * @param crouton
	 */
	void add(Crouton crouton) {
		croutonQueue.add(crouton);
		if (inAnimation == null) {
			inAnimation = AnimationUtils.loadAnimation(crouton.getActivity(),
				android.R.anim.fade_in);
		}
		if (outAnimation == null) {
			outAnimation = AnimationUtils.loadAnimation(crouton.getActivity(),
				android.R.anim.fade_out);
		}

		displayCrouton();
	}

	/**
	 * Displays the next {@link Crouton} within the queue.
	 */
	private void displayCrouton() {
		if (croutonQueue.isEmpty()) {
			return;
		}

		// First peek whether the Crouton has an activity.
		final Crouton currentCrouton = croutonQueue.peek();
		// If the activity is null we poll the Crouton off the queue.
		if (currentCrouton.getActivity() == null) {
			croutonQueue.poll();
		}

		if (!currentCrouton.isShowing()) {
			// Display the Crouton
			sendMessage(currentCrouton, Messages.ADD_CROUTON_TO_VIEW);
		}
		else {
			sendMessageDelayed(currentCrouton, Messages.DISPLAY_CROUTON,
				currentCrouton.getStyle().duration + inAnimation.getDuration() + outAnimation.getDuration());
		}
	}

	/**
	 * Sends a {@link Crouton} within a {@link Message}.
	 *
	 * @param crouton The {@link Crouton} that should be sent.
	 * @param messageId The {@link Message} id.
	 */
	private void sendMessage(Crouton crouton, final int messageId) {
		final Message message = obtainMessage(messageId);
		message.obj = crouton;
		sendMessage(message);
	}

	/**
	 * Sends a {@link Crouton} within a delayed {@link Message}.
	 *
	 * @param crouton The {@link Crouton} that should be sent.
	 * @param messageId The {@link Message} id.
	 * @param delay The delay in milliseconds.
	 */
	private void sendMessageDelayed(Crouton crouton, final int messageId,
	                                final long delay) {
		Message message = obtainMessage(messageId);
		message.obj = crouton;
		sendMessageDelayed(message, delay);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.os.Handler#handleMessage(android.os.Message)
	 */
	@Override
	public void handleMessage(Message message) {
		final Crouton crouton;
		switch (message.what) {
			case Messages.DISPLAY_CROUTON:
				displayCrouton();
				break;

			case Messages.ADD_CROUTON_TO_VIEW:
				crouton = (Crouton) message.obj;
				addCroutonToView(crouton);
				break;

			case Messages.REMOVE_CROUTON:
				crouton = (Crouton) message.obj;
				removeCrouton(crouton);
				break;

			default:
				super.handleMessage(message);
				break;
		}
	}

	/**
	 * Adds a {@link Crouton} to the {@link ViewParent} of it's {@link Activity}.
	 *
	 * @param crouton The {@link Crouton} that should be added.
	 */
	private void addCroutonToView(Crouton crouton) {
		crouton.setView(ViewHolder.viewForCrouton(crouton));

		if (crouton.getView().getParent() == null) {
			crouton.getActivity().addContentView(crouton.getView(),
				crouton.getView().getLayoutParams());
		}

		crouton.getView().startAnimation(inAnimation);

		sendMessageDelayed(crouton, Messages.REMOVE_CROUTON,
			crouton.getStyle().duration);
	}

	/**
	 * Removes the {@link Crouton}'s view after it's display duration.
	 *
	 * @param crouton The {@link Crouton} added to a {@link ViewGroup} and should be
	 * removed.
	 */
	private void removeCrouton(Crouton crouton) {
		ViewGroup croutonParentView = ((ViewGroup) crouton.getView().getParent());

		if (croutonParentView != null) {
			crouton.getView().startAnimation(outAnimation);
			// Remove the Crouton from the queue.
			croutonQueue.poll();
			// Remove the crouton from the view's parent.
			croutonParentView.removeView(crouton.getView());
			sendMessage(crouton, Messages.DISPLAY_CROUTON);
		}
	}

	/**
	 * Removes a {@link Crouton} immediately, even when it's currently being
	 * displayed.
	 *
	 * @param crouton The {@link Crouton} that should be removed.
	 */
	void removeCroutonImmediately(Crouton crouton) {
		// TODO implement
	}

	/**
	 * Removes all {@link Crouton}s from the queue.
	 */
	void clearCroutonQueue() {
		removeMessages(Messages.DISPLAY_CROUTON);
		removeMessages(Messages.ADD_CROUTON_TO_VIEW);
		removeMessages(Messages.REMOVE_CROUTON);

		if (croutonQueue != null) {
			croutonQueue.clear();
		}
	}
}
