/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.activities.messageDetails.body

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewParent
import android.webkit.WebView

/**
 * Created by Kamil Rajtar on 10.08.18.
 */
internal class MessageBodyTouchListener(private val wvScrollView:ViewParent,
										private val scaleDetector:ScaleGestureDetector,
										private val scaledTouchSlop:Int):View.OnTouchListener {
	private var mDownX=0f
	private var mDownY=0f

	override fun onTouch(v:View,motionEvent:MotionEvent):Boolean {
		if(v is WebView) {
			when(motionEvent.actionMasked) {
				MotionEvent.ACTION_DOWN-> {
					mDownX=motionEvent.rawX
					mDownY=motionEvent.rawY
				}
				MotionEvent.ACTION_MOVE-> {
					val deltaX=motionEvent.rawX-mDownX
					val deltaY=motionEvent.rawY-mDownY
					if(Math.abs(deltaX)>scaledTouchSlop&&Math.abs(
									deltaY)<scaledTouchSlop||motionEvent.pointerCount>1) {
						wvScrollView.requestDisallowInterceptTouchEvent(true)
					}
				}
				MotionEvent.ACTION_UP-> {
					mDownX=0f
					mDownY=0f
					wvScrollView.requestDisallowInterceptTouchEvent(false)
				}
				MotionEvent.ACTION_CANCEL-> {
					mDownX=0f
					mDownY=0f
					wvScrollView.requestDisallowInterceptTouchEvent(false)
				}
				else-> {
					//NO OP
				}
			}
		}
		scaleDetector.onTouchEvent(motionEvent)
		return false
	}
}
