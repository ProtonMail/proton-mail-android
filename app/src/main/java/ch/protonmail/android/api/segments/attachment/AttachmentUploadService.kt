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
package ch.protonmail.android.api.segments.attachment

import ch.protonmail.android.api.models.AttachmentUploadResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Created by dkadrikj on 22.9.15.
 */
interface AttachmentUploadService {

    @Multipart
    @POST("mail/v4/attachments")
    fun uploadAttachment(
            @Part("Filename") Filename: String,
            @Part("MessageID") MessageID: String,
            @Part("MIMEType") MIMEType: String,
            @Part("KeyPackets\"; filename=\"temp1") KeyPackets: RequestBody,
            @Part("DataPacket\"; filename=\"temp2") DataPacket: RequestBody,
            @Part("Signature\"; filename=\"temp3") Signature: RequestBody): Call<AttachmentUploadResponse>

    @Multipart
    @POST("mail/v4/attachments")
    fun uploadAttachment(
            @Part("Filename") Filename: String,
            @Part("MessageID") MessageID: String,
            @Part("ContentID") ContentID: String,
            @Part("MIMEType") MIMEType: String,
            @Part("KeyPackets\"; filename=\"temp1") KeyPackets: RequestBody,
            @Part("DataPacket\"; filename=\"temp2") DataPacket: RequestBody,
            @Part("Signature\"; filename=\"temp3") Signature: RequestBody): Call<AttachmentUploadResponse>
}
