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
package ch.protonmail.android.crypto

import android.text.TextUtils
import androidx.test.filters.LargeTest
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mapper.bridge.AddressKeyBridgeMapper
import ch.protonmail.android.mapper.bridge.AddressKeysBridgeMapper
import ch.protonmail.android.mapper.bridge.UserKeyBridgeMapper
import ch.protonmail.android.usecase.crypto.GenerateTokenAndSignature
import ch.protonmail.android.utils.crypto.OpenPGP
import com.proton.gopenpgp.armor.Armor
import com.proton.gopenpgp.crypto.Crypto.newKeyFromArmored
import com.proton.gopenpgp.crypto.Crypto.newKeyRing
import com.proton.gopenpgp.crypto.Crypto.newPGPMessageFromArmored
import com.proton.gopenpgp.crypto.Crypto.newPGPSignatureFromArmored
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import me.proton.core.domain.arch.map
import me.proton.core.util.kotlin.invoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import javax.mail.internet.InternetHeaders
import kotlin.test.Test

@LargeTest
internal class CryptoTest {

    private val userManagerMock: UserManager = mockk()
    private val openPgp = OpenPGP()

    private val tokenManagerMock: TokenManager = mockk()
    private val openPgpMock: OpenPGP = mockk()

    //region One Address Key Setup
    private val oneAddressKeyUserId = Id("one_address_key_user")
    private val oneAddressKeyMailboxPassword = "7NgO4d0h72zt4XuFLOUbg352vhrn.tu"
    private val oneAddressKeyAddressId = "MMxTCP7DMErrWkOREQljdviitcZ1mDzjWcnzdg8wqObisClP25ILZ18vUsNgUVi-JD3O2EgmOuiEmx8C5qmofw=="
    private val oneAddressKeyUserKeyFingerprint = "20cf363b58ec99e722e53ec411c31e8e5e07f4d0"
    private val armoredPrivateKey =
        """
                -----BEGIN PGP PRIVATE KEY BLOCK-----
                Version: ProtonMail

                xcMGBF1BfxUBCADUpiiG3AhQK08E2nBmQ50XeztOWArmknINQV41pqGFW5VQ
                kfbQ3FYsANhLGqbDBQ0XxmocjKL7W7W8Y4xmHCGgkCUy6gAqGbi+sXY9Sl8x
                qQNHuZDhWVdqT8+Rtv+DRxp/XrGkzC1U8CBYUmmKS92ldy0/zZIvgQXT6t5Q
                +v+BeUSv4jCsnY3BE0UBOljtrTXlOcXRZHQxORWG+kon0qgcJERdwwzhxY6e
                T8jEfAfJY0hzQaYg+6bj6ZR0zkMtY2Psq2M05kzEw4On/dezZETAu1e9fSqf
                k1mp+H6BeLJ9RUyrFK/PqIO48+pU8CmAvTdx5eIihyOM16CFg/3GgV85ABEB
                AAH+CQMI5Kvy7QRMRchgMAnCbvgFPP9UbdrivX98cJpvyi9za5FsYAE8OH7p
                UW1pMrySG52X76Wodw723Tq1qSFcZ6dTKYRuPf6ffrmg5pe8IJhvVnMauyJu
                4be1iCgzaSygMsD193bNelyd4s2fKa1OIdmh5mxVDdEgpUv8+6Xw+URA7V3C
                HpSdmELEYLtfSaO3m7IK5jO8WMgN5KSn/is9dztF2cuG2lcXY+P5Q4pFvL50
                FamAIB0wU8mlQPmj3KS3EBl34bLGUe3yYDIdXbfx1zm0REtx2IaVvt6tdj//
                l74gF11DNh1G61qMoAZEuGCKHlD42pCGtslkZsA9JXuhD+iVNDijHZI0y3gL
                /T5s0Afcpx5pSLdwigoQ/RnrInRlKb85xYnoknK8UjroW1ZibmUug0WWFDtj
                z16/AKrMMK3XYL0OTAyTY37jvochop75Yrpfve9R9voXOIWZjBxku50eVcRs
                mrLteNBmwRRHO5B/bLiaaP20auYlZL6r4fvvpoC77rKCs3pxDKlpQVsi96Kt
                okPo1xNUcsbYiHSR6NZUntU+Jzfz2Cn1t6e/mP/uQB/HRlYHzZvg9Q60zmM6
                1e5CF2eWTlQ0dHwPmgRB5gBy/SCUwlT/sZZN9sNupbzo2XMPsagQy6p1jnf9
                zBePypmjxGa4BX96UMIoL9a7rJFjo2LoBSEt3bVRq3e4mE9ZuBqfPc4SCXmy
                ss3XWPPwk5k37CAoBoZp241ZUNMSc5qxh6k8Pu1SZJZbWNAuQUjxTxRKLDzR
                rLZcEKnaimZ6Q90fhCuw1QbwHHL/jjkEsM90tW5MU1Fpr+GZQVSYJtVrSmdq
                POZ1rQdFtwzxm7uAunJHVL6Q0L8fodpHhcXokE7dqDAJzBXuhVCq/dL7ypHn
                JZHMFx3dThU74oQmT4z6uyjT8iKKlcvizTFhZGFtdHN0QHByb3Rvbm1haWwu
                Ymx1ZSA8YWRhbXRzdEBwcm90b25tYWlsLmJsdWU+wsBoBBMBCAAcBQJdQX8V
                CRARwx6OXgf00AIbAwIZAQILCQIVCAAAx6IIAAg2A2ZMkzGV+vZPbqAMoAEO
                +dpG+dq9C93Ui4HvoVHpcSTolVM522r81Yc48xdhbnFz9HLDkicoBzXo40ut
                gQ7bF4iKD4lQztfh6+9l+IBNu+1XmdW+laMybygtPh+H4YPxLZA9O6FYRyUc
                TjlZYFFxipz9pc9qI58tDHIILzfjZPCC6reiJpbxJOgp07PV3ZnJqLDIkFPl
                PkxyqymfuWHnPOJM5RxvHnu04ptsp/Z/xbgUra2JEyVLA7gC/yznxfQ58087
                pCupKqQwepA3zHmECS6vk7uuNp++D9JajjtFsu4piP4cTNVvMqnDXWn0uzwr
                hhw/fZnnHSllXmBwgmPHwwYEXUF/FQEIAMgCI+srSwdQlIpz+n+mlSpS0jPX
                vRYoL9QgMOdzR3kAW5sM1OW2Z7ROlBEZ7ycurpe4Sa/SaKfjtf4wOs2hmpxe
                cL9JxL0x3KGEaSeEIiYIkMb4TnSLR9vfowVdReOMTs5RpxMxQL+xmz3nChwL
                EIF/amAo/ucnXLbUNvYFkOpzdtxtN0dy2ykUvR9rsNUiGBoIn/BYCqSXpsCY
                7kom8lYl039yQvGVLWG6vryF6gExRbW61B3yjACpR6NLi2Bqta0SDRkeg5ob
                umxoWaJ7ltJ2uPuVofOpIPXP2CO40iCLKUUZB/r+/kVx+dYfEW3Nk4r+uKsu
                3CCSB9AZNJRQGiEAEQEAAf4JAwhfzrMVSONvzmCJ1AyZfwhCe8oX9cPTb4f7
                4LoafpdkKGgnWzoR1tco42SKtuXKmhhGAIT0EXMMzflphQLxvuNg8bK9sfPo
                F+XWMJJnPlWbVEZ0J8P0Ql9crsYtvGX7ReP/EEnO/TYMcRaOIZFySkVAOS1x
                1ISFbuh83ZHpmMXTWLrASzyHQUhxDnMA2H4rJ+Yi8byGbmvAf/dKl9iDIYds
                xur1kspeFaogiBX2yDXG6u1s1Gz+eJ+zXy/FNbeM6sA0SQSYBzqQk1Ffed2T
                /0FlWhTFTd0JvIK3QZVrN4nPQg/AW9XsOdCSVXs/4ZmFj7nlTeTK+fk0Hm0X
                jOLFzRhrkZbQ9/Rr4CpY//fL3k/1AVidWlb0VwKJTd6RwzqHSpego6SEeOPX
                KMPo6azj5yYzoRwdkRsbBXbxhWi4DSlEbHo4qoad382jNX/Jd5xXyneUHz26
                Q9WcFMTp3iWgKQnSBzYzaJbylTHFDGxPYwSbOT6K/aszDmOlLxPN470LlNQR
                Ln6CYg2dim/VWp++xiWoGlEen8eQ41DI10HxJPk9rpEK0adQNubDsnBP2wGx
                bzBJ5ZTx6lgWfcDHzpArqilLIxAJWUjjy5H7GYRHlqntOPH+Xo9fPt0TOsmI
                wf93MYc1of+r3/D3qPVQtXtCR3uuSmG7A6PTMI2fwoFSTSB676c4vtGEW1H1
                GpzknQvTO5b/13+BtarzgPibkg3MTOmq6qIDCGSxz/kemRepA9cz4ietH2j5
                ZCCpf1NuYlwvb1ZdtUs4zerjgZqdeerOTQVYJuyc167RM1rEOWUoUYfHt8FP
                WFSOw4KKxg6U1VpMvChuurTjMkd/Cm9F+9Dkky1kG41icRnf6/3nF/MZcHCr
                BCN5kjYKMqx4CBmBMKBBIBQZvkOFNZUarbjW2Rjt7ByJuS3RXoLCwF8EGAEI
                ABMFAl1BfxUJEBHDHo5eB/TQAhsMAACC8AgAbItodhOOJcb85EggCB1CEoFg
                6jOs5LgRw4810xI8HBPo/4Gk1L8YPfenMA1Uoz0x+3z42d49QU5HZ/hAmtDV
                W9KP2Sjw/axfsgB7v6sbrXgtB/OMblHXoqVJU4wVbQrYvxnG6YN1iX83QGGC
                1mYHWWDXFjZM8egN63Ocyccbywvq7q/KEaXlrqpxbaDW6uUXRUX8ISqDWXAA
                qEUcgWI1H5fqMKODQolr0yMBbqggI7GhfSOnX3mZaLHqy5ElJZUrXi6J5Pq4
                vnJgLm1kzP632uztjEKQfEVFPUflksdQP+v3eWKpb6nNTH5tV3Pmo0xvRmic
                dlEt7f8XNvX3HxQw9w==
                =FW0u
                -----END PGP PRIVATE KEY BLOCK-----
            """.trimIndent()
    private val oneAddressKeyAddressKeys = listOf(
        Keys(
            "DvRZxrFRFUnjsL6MCOdGjyMZ9AoECd7kNXX9uKxmV-75K4iArEHijRPvd7Dhw43yBlCDxIsbNSW7cg2Uu5FUFg==",
            armoredPrivateKey,
            3,
            1,
            null,
            null,
            null,
            1
        )
    )
    private val oneAddressKeyUserKeys = listOf(
        Keys(
            "ScOOyl7_x7HXDuIUGUzWaIz83IFQJKWHi85smpaV-QH-4gVnDBwnWBtDixsVMfehF-ZGOjxrbUfUsmm7cO4R5Q==",
            armoredPrivateKey,
            0,
            1,
            null,
            null,
            null,
            1
        )
    )
    //endregion

    //region Many Address Keys Setup
    private val manyAddressKeysUserId = Id("many_address_keys_user")
    private val manyAddressKeysMailboxPassword = "ikaAA3dimv9p7D.bqZ6mq.R45LRS6oi"
    private val manyAddressKeysAddressId =
        "odHOhMzF3YKgGR9firZjLxRWn9lfF2CPuS4XX2t0VWaKggxd3qqQ6QWv-hSbavF6VjZRmuFdDVXsMgh_lhUX-Q=="
    private val manyAddressKeysAddressKeys = listOf(
        Keys(
            "i9DnLBbuJ8VfUf3ZG78KSz9vr8A6Tx--ylLAWOSvlvXskdW8deVC_UVA_Yvx6UZf5xv9vCRjfwNkq7JrJuiaYw==",
            """
                -----BEGIN PGP PRIVATE KEY BLOCK-----
                Version: ProtonMail

                xcMGBF12FxwBCADZPDcFbXjg+utHsg9RCTgUkIdTUbRzxme83u4r5UqxVqIf
                coUF4at5uKPj1XVXAtpTxEDsJ8RfseGSZHTUxcwkYWUDL0YyynFY79GXYd6e
                1tAKLzXmlNPHBafKoN2gubXmC2zQMjPvWL8K4Vqg9yhEFaNlKRSnzFN4n6Ih
                G92Yz8pyid8wBUcbGtJ+REGQpMvSugRkpGzz7p75IGLSkIaMlvf1AnRrUKRr
                MB8i8UzkpyI8kjr740JmWNqxpJWzczkSy93pjsYB9drvoFBViai98EGa78cT
                YGK9LjU+brWMu/9Vw7nQQERG48ISePa5bkdSzNACwkjvSC4xISmu4KuBABEB
                AAH+CQMIKllGXNYMSsRgokZQfPjBaEOUXYcgDeliPaZscv1yjTdiKZudWIub
                k9ggNV4C7WjbQA2FBKnpgF5aq2a/H8Ha1uaGe3q3I5mD6vOO7NM6Lo7gH2Xh
                2/7xe+hEjqv1HoovQce0lSAlxZDbd9Jv+VulOzaD4LoiF1cdPfaS49cw+nKe
                Ge2CvVfDwbwWh+z+93nX/fD8UQQXDFqEA9lOsql3c2SDkmyLC4YbJfohdkwx
                4FI8EgcG/8HOq1CP+dINdLcb0bPZs1ejteDSIvdNapvzzUBIjvJOlDj157AH
                ahYnFzh727+CydRtEKD9GTVvfREEIuJxUBxSEVifszKl9dEkMBbGJkURYA6J
                iHCnflLVxGoQKl9VQW/9EOzYr8/t475le2mXs7pMcq0dfYGrM+BsLz5h7G5z
                6jeJt/bF664rLYvUNI1XZuE2oYyDjIA38IMPJvh+9nNrNEdj6+enSPwnzhzB
                w2ipEW9nDz2cJjcO3RMF+AkiHHWugh4eZFTaI3t2XO6itrcoBVTJqFxKYyQj
                53/ES7sAe4OEZV4DxCyesj0372+qJ9LoHQhQ5zKI0WBHy5UthWAJV8y/HZ3Y
                tb9OAxcP9f8k7FLvg8j90OAeklsL7yZG4U4J2UjzNh+SKh3jK/JFsIZuxg5t
                UIUOqfoBLmvsszuqA3fCsmNPq3ttapGAI2pNXRJLqAmi/XA2Gg++hHYAJrq9
                kjS46OzIaCg5Wi+0ANjBj4Ub3ra6awInSol6IIZfYeTSjrT6wL8NRhrQngD+
                NTQ+HfLQaKp7A5YW37AZDck0CM+oyVG5F3H8pMSZ/pKqEdn3oFUerZypuEUW
                UzOR7cUswxF3svEwjljMZv8kLrzuun2x90i9qTg8wpuNGvtrC7UGBRmqnxR4
                Rv/9uA6o623oy0NP1kh151dgVROlqw4vzS9hcm9uMjJAcHJvdG9ubWFpbC5i
                bHVlIDxhcm9uMjJAcHJvdG9ubWFpbC5ibHVlPsLAdQQQAQgAHwUCXXYXHAYL
                CQcIAwIEFQgKAgMWAgECGQECGwMCHgEACgkQ+Krxzrw0Kbw83wgAivaBBVVk
                7M3GpMBMWUBWhW/S+V4BdTu8QuKiEufUbHnZCkpqQppE2oSazeg9UY0WRwDk
                g62q43NGdaGHQpJdeg1VKgqx99+JLnpu6x2AzU4SRpQmt/+SBpwEFEI4ryJP
                UDwA5AnF1TdmbhOnKSlU9hyRhT1xRT5Tt3eQpM2V3yAlbmUxLliRWbMX94z2
                +i4Z7KFev3UlPVnaI/+NLgfaWJqTaL8JIygpezCqUaA7nKsrKHLKo4WqIAPv
                SpdKk2bOqju9B6sQfUBuQudiLQJWKBDqcpXW8G9RHjdk3umezKNsRVKekRGR
                DyMlDxb+mQHQfYHtXxJutUiFWRm+aUpS2MfDBgRddhccAQgApxoM6zdh6+pr
                LbH9PfUIitpUfxfj3Yct9749PnTRCNiWvnbcXGr1pZ4mg/DFtbEvX+R4yj20
                /xOXmIhgZrPsFsK5Di0ug52GiF0D0FosqX/ROJMiT3MisKGV39XUWhgyaiT0
                dk2FGiJ79UfeELXUncp7mWHI0qqQDzruShdK4ho9dHP0uO7QZwbWcN3dIr63
                blPZXQ882438rbUJxW1ZlNKq0smqaf3AQkpKkvK/J3feCHrL6AQgHURne0jB
                Qr1WdVgLRafKLYYroSMCt4eqCYFT1NOkK7H9RWTzHuEHozoUHzr1PGos2vzG
                euPPU+QAk9l0NauumljPPJNgoFdjiwARAQAB/gkDCBH8tld6bm8iYDwMjFZW
                ecztGmiu1Y/bnpezZF4Kn+rHSSeteN0cKWWYGMC5KF9Pd3XlluRWRLGAqgPz
                FNyB1fseNZrz8c5ln10AS2LYDzeVRUqyjx5IeTZRneGx4yjq8u4rw/V4cGjM
                DUB+GSBUWe4yzCO2dgMjAnPdoalGYB09WAl+aZkpS5XSpOVmDSYLEPZnZuD8
                V/OhjgMkqMaxzV9a18HpVNj75hhIgrALgqKofLdVgim2a1lI3GTjCEP43mEs
                78saHrMYO6F4nqpOXhCATzB6VDwLmGK0OvKEAyRv0kM9s8Pu/oG8MMjzgV8v
                TWrr/7YDtYjGIFPPJ3/c3NVCx1QS86K+tnWsPS/px4HBu4P+vUsO/j2zbr1E
                Ix53izfjO8HZ743wZdO4U02fvAAeVo4RFZuRVd/zyaDD9ieEo/vq9kxZzN7M
                UuEpoAVKtYV9RQY9o9AfdrxwMNqJN9NAM3wtsji8iXvuH1mPAKuG6X27aZ2z
                AJ3jvAm9SvA98tUFUb2zPyX5kdBflBxHnjJzpSKM9wlqnhaFq3E91EhrzZY8
                TrsSzCB/ObaM0FeitbADd10dnpGVjMNmjDUkWZ3kO1H2vTDaiz5Q2DWb2yFA
                bYHQgXa2seNre0nOkFxeHDUxjsweiuHkTaK//o2idxvYThAXy0CQZ1Te9VN9
                Ed6I19FSB9Aw4p9Ap/mXn21pwDFFnZgW1YunkNONOCekw/lKM52YQmsLDaMC
                AJGA8AopLKjx4bpQXc1xRev64aca1CxahSkPvGjs16ICNEekCfTR2RADD3Rk
                o5/8F6/+Om8iebFHXi0JTXk4zjM1kKLaFgN9gDVepgUBdB+pMPhOh3vtaoRR
                CA83toS7juI1Hr/Y0/ATxdt74uzmw4Ll0Y4cr1Gs63sw9zOD6kOKHHecV+/o
                qnMbesLAXwQYAQgACQUCXXYXHAIbDAAKCRD4qvHOvDQpvDf4B/4g4wrRhycv
                m3CmnaDI1F7X0JBYp75TXhpFPm+8Tbze3lg2KLYtpO5Ut1/o0ugcnNLE7y14
                KR3ZcNbwuGbxrhfeWQYNadkOV2xlIZntrkvqpuMY6XbinYkkcdVfDJo8kZ5q
                iky+ghv/UF3VRC7PY4E211Ic2d3nI6Rn1YntFpFNSm+VNQ6hleHDVcPNG1vg
                8VcrgENsFcC/gfmeGgSxDCTIDItHHF1RyRR3x7TJ4Z0qUlN1G647nT6GkO7o
                fGMsIHl9AHQ0fgmGZO8Og5rbyUxOT/HW3E/SoSG84B2OuaxbLntxMcNS9u9x
                a2j5Ak+MfeXknuaYqP6hs5BzulMTTJjM
                =4S1O
                -----END PGP PRIVATE KEY BLOCK-----
            """.trimIndent(),
            3,
            1,
            """
                -----BEGIN PGP MESSAGE-----
                Version: ProtonMail

                wcBMA/AwZ+kGahlZAQf/fkz6NVnIhvSFSMA5GpaakdUC57vCPZa3Zf0VLJZO
                j3Nh3Xn1kIvFDGe75H4HZsAGJFFGyFkjKIEoyR4bMOzK1M7vKilm6O47ydnS
                hGutVmAo7nHEdbgDjUEnwJnePwYK8IaTpPK7QjRxBAI553NQNLM9OJ+byluU
                /bR670tG4UJpMkA8eEvjpHS6+xLngq+HT3PM81QsKUwBmkRFVHyLqqd3y1yg
                GSTFedNqOZae7CXHlxJe88wU9n9egZPiIsbRErf4ZJWhVbPQQxYx4cavVa00
                QncdJ00QDWStjZFZucK8RZU3UPfhuV1W7FfIhtp/x/+hf7iIX1Y4eVZ9kjR0
                /tJ4ARMLQc/NBQdHCwmMHYGJZc9r85dX6YplrdaROlvc2f2oEKxUlH0MVQoO
                qpYay7uM+X1ywV1NJNlrZsSDwoeR+lGRidhq/Y4r6K2g/MJCJv2xSK+Lrp7m
                M8EKUXj+dFaq00ViLsBEYeqnRQRsg4GRjEI+luZOT4u0
                =Xd62
                -----END PGP MESSAGE-----
            """.trimIndent(),
            """
                -----BEGIN PGP SIGNATURE-----
                Version: ProtonMail

                wsBcBAEBCAAGBQJddheAAAoJEPiq8c68NCm8FN8IAKV+VmWYfkuiisCsWkrF
                VCU3sRxz//Gno6sZl01bOoJJ1rDbJH584nfVoLP2mmu12gcO6nKWVj7eAN0j
                FuKp9ZEB6XW4LdxVd1KAklTPYN+OePvbZBgXoH/OX4+VSadkBgZfPUeZ9oNU
                WcBjzYWEZNWNCxRlDOiaZX7q4mNMlnvhZF0zk3/6g88CKToXlhW7f4iJ36EX
                wYnWjwQGhsBEJ3BoYeYFW2M6mXlh6Fiembe9FZmORbhlCpJ0exPOC4rKg8EE
                7XKWrrAVncwhXb3IbBPwbsWVS5yBziaiw1Ozo6HyAzsBEskLRajBjZoByO8x
                TkchT8r9OTDm26fYGVdGiQg=
                =68Qs
                -----END PGP SIGNATURE-----
            """.trimIndent(),
            null,
            1
        ),
        Keys(
            "QtsLAEJcOo_jhThoFLMfCQrWme3lRiQ16TWHwE9O0Vr-4Qn-3xSWvHGzYesK72--nTwebC9ZK01RhMd16ymDFg==",
            """
                -----BEGIN PGP PRIVATE KEY BLOCK-----
                Version: ProtonMail

                xYYEXXYRuxYJKwYBBAHaRw8BAQdAtZY+Jb6pR3lMwGroH7k6/eG8UrOql+FV
                3pH1Bd/6xCf+CQMIE8QkUA7nY/xgLDtKTJ0J4wS3fgT5+0/ztay5u3PRJt5m
                TUVOZzhd8ReeDOu4xsP4exFGMy/CVu6A90cQthyaDxJGCMR5c7ySt/OwfU7k
                iM0vYXJvbjIyQHByb3Rvbm1haWwuYmx1ZSA8YXJvbjIyQHByb3Rvbm1haWwu
                Ymx1ZT7CdwQQFgoAHwUCXXYRuwYLCQcIAwIEFQgKAgMWAgECGQECGwMCHgEA
                CgkQgovwMGfg5xzb9AD/TQaYoyas7x0lbb/sX/AwWmPos4FOoj2Bu+IYcblr
                b/QA/2Fdbxgep0qSTfDWW2cJ465NLekrj/BSWuBkUmFc+AQLx4sEXXYRuxIK
                KwYBBAGXVQEFAQEHQMjPZ/LK0Vteukg6Ah0EyV4ClzjQBS7Bf/Ay0gbh8ONj
                AwEIB/4JAwhX9ycQRkdaqWC6Vtauscd8gYHgTdTosb6ANkVrrQd6w7s6u7BH
                Abe3NjrWcbQo+FkXbca9r/+YHqme7oSwF0Cp4QATmXEwAf8aIDvWkOTnwmEE
                GBYIAAkFAl12EbsCGwwACgkQgovwMGfg5xwhvQD+NvvdyEEY+4g1tkp2qC2y
                q0SamQiEOg6/yz5gzwPEatoBAOk+AJwP+1qdJncILj0KJ30KnfBHbEa86veO
                z7XCwucC
                =GzAB
                -----END PGP PRIVATE KEY BLOCK-----
            """.trimIndent(),
            1,
            0,
            """
                -----BEGIN PGP MESSAGE-----
                Version: ProtonMail

                wcBMA7M4YhTWmh7GAQgAi8nn7wQcb+bh0EZknNdPHMJ3D1QLijgTSv/WfD3p
                2k14RoJReYW7/gvPBUJQKC+unZGuUDrceSJsAZskBGTnJ2ZLNVkAjSljUl9l
                za+XYHKfJnhrVsF9E1edeh6KY32nd7F11UPcJ5/7ca1kZCNdUIaAoDTdPI7B
                wpC+g8A6vylVJ5P1gTKRPi+YR4inRPIxXz3f4nrdrZWO5ta4Yt8wSwgF8VRx
                KItqunhuphaR2E66aUftennIrj8SQRl+ExqDRRMOQGpwF53UnZBX+JvMjq64
                tFDoIheOolCCt6yUa85iLTuCiMLNms+Mz/T8FvrqUs4ousKawEi6cVYK5tca
                CdJ4AUa86f/szoTH+oWuFApaLzK5j8KYQF0Z7YB8P/TGkkGrtpbl6ldSCoc3
                UuFCTHEbf+n/AbErz2om6UojgPaP7ezzpOZdCCWI1GG5o8iQodrUstbe1SMR
                ++kf6m7JU1dU/1tC1PjOxUoo7b4DNB1PGBLmH1uoSJlM
                =pcs5
                -----END PGP MESSAGE-----
            """.trimIndent(),
            """
                -----BEGIN PGP SIGNATURE-----
                Version: ProtonMail

                wsBcBAEBCAAGBQJddhY6AAoJEFW4yVpiddmBBDYH+wW9RXwIqtwFo1mE5T+z
                Wq7rxWJKu7qqeSTWmgMjhBEjrLlvD2CzIH4LZw5O+y9iVye6/4bsBMWjHgGX
                Rb59zR5nncqIDyKQu9sbIJYzLyl9nTnes/MgUfLwhHgJ3BDRKlg8qnxepBnr
                i6cin+Z81kmaQccHFSxabpA7g6KCdFOUKoq9eyvaKZIWQ3lgaMqSpJO0/3WA
                qP4/vPwgbVsPR5X8cxnP5fkW81l2Stz+fQKdznTM1CG1ImfuWKXxq8huXsYY
                wKUdbdKzG14vsCrZY/UUNq/o9515aRojjV2KgZByq4KICra6jPxkAnvz8jRx
                ulskRhRJqufYuGUUBmK8fx8=
                =4SVY
                -----END PGP SIGNATURE-----
            """.trimIndent(),
            null,
            1
        ),
        Keys(
            "Kufr0UxSSqOB6975FEPC17yNhhq6FyE_Jc18-O_Q3w7bB3yWgC-hgJrxRF2iJxfqTDAwMjtvv8_roYJhDpv8Pg==",
            """
                -----BEGIN PGP PRIVATE KEY BLOCK-----
                Version: ProtonMail

                xcMGBF12EaMBCADB814SVkCF7NS2VnvVQwfGnBfpGIr984PZOMk1jpHgvcuY
                SBCHQjJZToO0mG40HHiTm5xI+9TXCOglU7RrAK95NH/YAEWaa7tBfTtQ/+pX
                1ZvnTwvpxfA6v+oHnPjhJdGhnhriElnJdv2pCShPkAi+XYYf+d4HN2YGeHTS
                7RnKilhSiHvHz//Jt2rlUefdKnUSLJL0A6eDq7e4YZbQFV2epqiYLsuxYAce
                9UJjHaD0J7iG5X9e452cmDJM+Vik3UqsvEXxrP9OaCd9QPoad9VHIWXzH1G/
                xKTjRnlqJC0z2AMAPt1JzKOW/kjinBLdhhwmKgf766Cq7hD1ip71lvHLABEB
                AAH+CQMI3+ScrtZEMlJg6HyC4vMFp+G067GvZfoAWFATmP7q3ZBeuilSn252
                c13WeL99ONn8rffTOqNkxIEkIp0ejnmg1PzRv0pVf2PdYOIO07ODNPesW3lP
                LmAMsBgFUUUZqov0OuByWsKki2/JxnQhSA7aCX+3+3HpsaWnuczW2VYCtEJb
                YAfAv6q+AUQYYgS3TaUEmeCibsYhFk26h9lY7gt3xk0q1IZghybDFC9Bk8Uh
                zeYlHx/jBOKhSbk32H2Ug8fRpYkyEUWxD+B8sdoVW70YLtPJZKMtpaNnCREZ
                lWMnorS4xPr/GjVmugCSqPEjKPd16BBuTTne5mV2qhohSFGg7/n2Jrk5vK7O
                jIZBbtS2R9c1fACZHePOpfEjxr8NPcgyDSaFh/et5SMUPvqHBgjNhlLW941L
                Sc1KZy3Khz8fNdeVEQANjfNfiT76vfIWt0cXlORf+Y4TGnDKjGqpL6FDZJDV
                oMAHDMlwVEclK64P2TihEB1BL/h1p7fhRvTXwWJ+eiJi2m3YUsFlOFx/7kkA
                Scjq+zYB6zkZxBL/XIuE0NrFavx1zU5SmyfAoKoxGWI99w9YFN8VCazLkva0
                EUNsKIiXRG7ss9whJZ4Qlq2lLRoSnZbLL5Dmyv3ussL/xAfv/rc7l7MjKT7N
                BE2Hj18P7/7A68aK4csfQEmd2DUvFU3KKJG39LWiyBf0VvAfdgw/AADmyc6Q
                6/DQMQDJDYmIAQNsWDNelmRvDYOVMhE8zmZ8wq0IsSuZ8DwRu/sCLPpQnszI
                pVdHMVHiM02DWv9xvg62IDB/B1/dkf+t5TBFpl7pBhxJBtRXcmnxl3h2REBI
                j57ccwGwsQFf9PXOHpARvbcThtlGlPhEYeCvlZMaPGJitvDpR/XsYkn1UMDy
                1gfCP2uONLdpQTZK/JkesnwyAfHylZNezS9hcm9uMjJAcHJvdG9ubWFpbC5i
                bHVlIDxhcm9uMjJAcHJvdG9ubWFpbC5ibHVlPsLAdQQQAQgAHwUCXXYRowYL
                CQcIAwIEFQgKAgMWAgECGQECGwMCHgEACgkQHRYHuQnEe6stOAf/XTnebfNv
                JnjpSh928SYvcv9kp5fkRtULFeWVB6L0UzwSyOsvlHzbzRW8aM8ogRzwn7CP
                9dXPL/H6WZRSyX/E78eXOstl1TDwpMovD3nKntCuOEaV7ApdAYrklBC13CiA
                z83Mr4+9WDwJsQFQDQc/Bn4GY9hHwcdKFDn4AGDHve+5un3PVns49GVhQWBZ
                mUapyiRAiyZrwkykqo+RKqG9PlYK4f6R/m4/tAHUVoNrpfkTPKh1xxLZ+fy8
                WxUHQ7RenpMBF4Jwe+hhjh+krJ3BOOe6T+8Kv2VZL4oao2H12IevPoOe2jCo
                z7W7kAsZwmPfLCMGtqVcRmX8GE7inSh8NMfDBgRddhGjAQgAo0bx4jmISLCe
                nLi69WU2Bgbzoj5LNAvBCxEF5DUBhhFsLkFbrxCl1AGsts7yOSvUOIz09Q7B
                s/UmY6M/oIMJj4545nis80663I1VjmOobdokka5iXuovQmGwvzwh0YQWo2pD
                JuBC+8DDGy+FcKecAhbXq80UWUYp8XeHkW6diqEmEaIUB/q5wnodCZfFm0pE
                AwXJxj0jKCqQh+rPzNuWzzgxCTLoh7vQ9qO3YqhU/QQ9kfS7I9WcgCVDce/j
                Xmk6pbP9RTQfXF3gERuURfAdw3qzwNi8uIHC1zfEjjA77JI08uoYbAz3vAds
                MO+vEDyuOMaBXuoynf+u79HWU2Cc9wARAQAB/gkDCIR9kHF16VmEYGOfX3qV
                0z+qfRLr45z6bDoikTouJMyqhyG5ANvAG29svnd+nVXTUD63skXqEjVtvT51
                F7Icqn14bj/SY7qAyUkMwiAhVGVFym/EtK/WDIGzgYZ126HiYtFXxIHBxZiO
                hwrWyaK0YgJASI24TdGA8yEHtMFNo7oTRfslR+uyst6bhQJSqIm8RdVSnGYE
                K2oRd46qBX7Lruod5nQvtMl+u16QXtVWSmeANplINVtkh798/fh3J9c2Pqqp
                LEfKfaC3lgwXAaeA4Um9KPnVLH6PgmOtlpHcn2IZdKVBtiSIgNrj6PJdA4Lu
                lEESAc8TQjKNYhgXghtQk6h0aOTOXeuzsZjRaPZeXKPnPWtBFaY/YBrgW1E1
                w5Mk0VDoUAGcZoyVxKr2NsmiyYbRzmlQCUYupvm6O5XcIwhHCa5soRWYJFMl
                OMaIqXGeH8Jw2SayaP79m3Jx/2J/bOGH9ds3HxxLuIuejlvxI1/YhHkmfpjd
                GbiNFBi7MjVY0eT3UH/nrRW1kMrJWrn0IL7+Mq/z9/nbbQcQWY/zirgiktDF
                Yf6M6Ec5VqKSVIt3luHdP0dR4DYpVJUnl390DgHbEQ3vKK3SmSTsS24g6usu
                B3+rWEh5ItIxV/S4lYnquRyNV6EXsnU6oJWGNRhZ4533ljqx7FLi9Bah/OVQ
                ty0Ok3FvGK9nTwumGaZwagLj2iwA7G8DY4VCRTPVGxEHd2UZPbOWgaqqqTj/
                TgQQOx271K7MEOkKnmrB3frX+SKP97IR57cDy0k3gwkYZL+lhT8iLCQEykFG
                7PXL9ocJRPn0cpeEh6Mb+pdLYPyTIfA/AP7+0jnUYbY5a6/z7nGYPe1v132M
                /3JPAdt0Ac0RyUK3MIjCFfoHwnFlL3rodrqOBboJUq+Z8f4elTlwTR3cSRW7
                NSvxL8LAXwQYAQgACQUCXXYRowIbDAAKCRAdFge5CcR7q9iFCACpWKY9wNPw
                JU0xMbTgOpIDHxU8KluZ/X8sCXahDN83ONdoBrS0QmbWUefrlZHNfWJNRYkn
                8BxUAbOdBl4hHiJ8Vlg36JckdhNu7UMgVitV4XoKn9+9/uIgOjzyhF3txFHK
                IDypxPugXb6v95tAkPQO8XFBrfHF7UzSBouKFkHHDSgilQ8HC0jxj2Pg/S45
                JlK1EbX3Zo5FaaxIEXE+cZzrZDelArCfrQHwOxr6Nh9NF0V4gl/FgXz7TYob
                na9Dj092VLDzF/ElV9z+2FO101A3WIlp3MkrvzX/Jk8mUaa0WbN44STmu9hP
                PG6pu/4dcMT67x9K7G/QEgP/AD+jTSMS
                =N8jj
                -----END PGP PRIVATE KEY BLOCK-----
            """.trimIndent(),
            1,
            0,
            """
                -----BEGIN PGP MESSAGE-----
                Version: ProtonMail

                wcBMA7M4YhTWmh7GAQf+NpXNbsOlshLPYAbyuRYKaOfCz8m6kMirO29XPXcj
                1c9a1ruZTC8N3y3IVbhmCRQp5ykSDmv+yUGCbSt45G5IgIeOTFJLdfD19QHr
                dpnhmlziohqmCciLkbwRQwXbfLP4rr5n1RDIOf8ZiIKGNWrRJ2CIExhW9dM0
                Rc2Ijiw3i76DyPpfELTjYxOB5IR9MhDGt9UHyJr60N47RoQQiibfwu6qWq9V
                1xN7Kl+Kl5DRQFrYWnPsXYRKNHGgWZFiDRaMvzLjQLDsWeDn2+bUIwxVzoM3
                N53MYkkDMKOsHr/RUCuCuGVGP4kRcYMH0MEvwCixhyr6GZvCyIiEtOFMj0TS
                Z9J4AZ8FMkHaF5wzmlURQn4+NP/XmFjqDDcPgjrTTcIU0VloWd/Rcrxtay54
                2lHK/zrzMYkrRG7jgZvCzBoqzCjMEVbNPv3qfmz45DN5m+OfpNkN7WbYFaqH
                sPBRgjr9+HxsyqPu3/wdawgLlI0c5lBrhivNojZBtACj
                =77u7
                -----END PGP MESSAGE-----
            """.trimIndent(),
            """
                -----BEGIN PGP SIGNATURE-----
                Version: ProtonMail

                wsBcBAEBCAAGBQJddhY5AAoJEFW4yVpiddmBjI4H+gImMlzpBRCRnDaAYWtm
                Io59SeLCUYC9ML2oqIuwcM1yIfvBhpdmxNuDGAmGqazJAzLUpeiV7iz2ymdM
                Mk9NxHngP2dkT31q8ZdV7rvGItAVV2lgpLFHcgWPMkdCo7a3jgbbo4qbgc4p
                Z2+2KwHOo5NIrrjgZHRoRWrx7p96OjqWZ38buvUQXl+bjhDLh0GBYRGRY0fM
                dV4FK/W1cu//cwBgjKAOmLhxKNMigSxDJMY33DTPOhaiviqpKVx/3qw6EUgt
                nqLuiSwSM4oUQzQnbDluDVVI68E0JlxJC8KerJWP8aeq3LvyrJgbNLSSWBju
                LgA1n0GjKKT+n1fJR2/DfUg=
                =yh1P
                -----END PGP SIGNATURE-----
            """.trimIndent(),
            null,
            1
        ),
        Keys(
            "v3foSmcRMfxukR9VdZH-ql-sm4lFfAkHYbgsCzUbSrHQD-49BkEQVlmgkWk1u8oxMkceVW5Loaxwma3RSFTK8w==",
            """
                -----BEGIN PGP PRIVATE KEY BLOCK-----
                Version: ProtonMail

                xcMGBF1ycrYBCAC0pSbZjrqLzSunLBItB7RZVrQgkvWBP4GClkC2KciKM5DD
                eELgxIc+OkMKgA38j42hDKsaQBZ77ugbT0GEaRRZeoINCXuna8tZfLMC8yHF
                ha3aTa16Vh23FJl5tTQDUlU081R+NpST9LjkqIluwayax/WXdXSQUBgVFDuc
                kg0zCX8U/nqmx6mRnwlE6D30dSqbB0tCS7SjkXEgOLdRpZXqyaexvzziJaGF
                Rur3GDpJwatnjepJdsrtjT+NppolJycfqbeNe7AodBQaPynQb1sLs+c+NMf2
                7TpMjOjbmHN/ReW3BZdQDX2JkTFmiDlfvy6v1jzyQ5uPPRi0eqLD7hWfABEB
                AAH+CQMIk4rC6rXozxtgkYarM+RVuzY9xwsbuQeeWGLhuvARQMoxWI58kfbx
                csKkX0oxTI2nkPB3qEuJMJ13TUsBblARxTdchG4Rmb6sILBkC+h8/MNXR0GZ
                tCGLUz7p7JxvgKaoruESEgNQi31Zd54w6v2ih4wFaRLkZ67Gx9zPcDU7yTCk
                NQKp7ILlurlGaobDC8mVi2+filzIr2cGrvCyLXFWoxbBqCjceQ1Y1Bo1JUvV
                GHchh5Pu7Mj01/bRMm+c92r5jAEUVz6MEMhxSQqm+Qql4ZG1wVokaC0rgS3b
                fxtRaYZAJdAfrySLn18R/t/2QKWlmS79qKRC30PbfaUiUykTc3Jxvf3JOmyu
                1YfeCmowmNVElWV3j0P6suX1urUkY6wKhd56VhlPVetN87ziQQPmbd9f++9s
                MmSr9Yfg6YJd3ILyO8UU0sHybffVGAA7pSHiAPzTSzH0MbzdjswZ4U4VCcCV
                u4L69oAVVzjoVCoEuTWzC4mj5980PP+AY+RfZ2HTCCGBa7XaJbIpanPbP12Y
                BWMGE2GiCLALYaXTwz8SibC8Z2MnjDmLO9nX3bn5fascPoj1KtH4MQ0sbhW+
                qaXRu56v85mF4qAjydtWzYHUYvwIJzJdi2m+lKvUn5mBEsqm12yWl7LRlHr/
                jjIV3TlUqt1jFIcA17DlYoQU91ZKU95jR6z1CfMSVyFMOOPaVaYSrUqFJkJo
                ATVJYSX1YA43irDpemg/yNQYcaw9Y4azAYnOULNLu22xa1c/ijYA4Hy+vIvI
                N3BHY7yiJR/Sk6YTwXExsbBox6SMQNCz8CGbrjprXT/2g7gqodZhvs1N4jva
                k2/tivcF7X+biGIV54R8cO4SSLOEXTui2R+42B/rUCBwtYDowO8my4SZy570
                ZQxvjoN3diwqDzCtJnsVLLt9S5hG2OoWzS9hcm9uMjJAcHJvdG9ubWFpbC5i
                bHVlIDxhcm9uMjJAcHJvdG9ubWFpbC5ibHVlPsLAdQQQAQgAHwUCXXJytgYL
                CQcIAwIEFQgKAgMWAgECGQECGwMCHgEACgkQVbjJWmJ12YGvKgf/YUqyEui7
                85dEkdCGUR8ekJyOrdUSBNFsctazWNcVSXSJ809YziM86hoGG/gTmz83PgCn
                jpXr1DwhklHt20nyx1y9cRyPaw+hBtastknIFsHkUYaBeuLayytuevsUMZiv
                Nmox/3fyRpv/HLJzM4zzdSlvAnhuKvyInDB7Ut1VtE51dbomIby54Xs5+CW4
                tqWCB3PWEaW54La+i790dbB6meFuuDAHaR+BeNDYPRT9v+6opeUo2xifwDW6
                OjNmrLeN9rwhtXWXz4zzRZc6V4VfilbV5dRVDN273Bh0wj7iHQJnwGG1B1S5
                K1Xh3WQIlo4UFyWE+znlCPalZvgxmxu4OcfDBgRdcnK2AQgAxMCdP4dF2jSf
                WbVyAJXtgSEfjQyusoU6CRq/jUbSo53no9vJAMOxDHtvJ+54vOm6DY1w1aZn
                Rnf3chaAlAcE0ZTnyfHxuPZbqs5peyAMtzfQ7FKzlAuV0lMNJ+g0nC+hB8jW
                E7VWhwzUgLueD89PMFlG+TUqcwamaYXk/AOL+IIVbOC7nUXDkDuT091D6fJq
                hff52moElSdkSEnMTKAWONhrYrutB1N3V2v1WOASjMncorWzksl+pvk3jzOw
                /qlsHxRi4ZBFAkW3K2Sigyx7P98Jd3bth7zs9H9YMpX8y0z0UOR5bPjBqsdo
                Bh/4wFo+DNv7SmUpMf0jouW3uxQbgQARAQAB/gkDCDJUfpxsgX/wYBzaVes0
                kYlOkmd9+n82nvJZtLwQVl21g7sTRAHZD1zyZ7dSDihXmiDqYzVbIsznVTzp
                A4trNZhIZDAQeUmY1CAQsabAn6UhCjg+SUqabQ3eGENtcA5HFyyRC7uRDyEr
                9eeuvhRtYDy8mWTgDg+9+M/3Rm3mhVre7VknSsW8rv6bYHu9v0cChHGWkk71
                jx3gWeDVxbn/4XTEE5QLWCk+XvQGBV/JtuaBH0OVeYNjuqAgTqk1bgmYqRO4
                ZkuoPqfaPcb8vQBCh0ObH7zZMMAWQOSC8M4x8cYc4uZI/2Q7mBzGdQA5qagL
                N/8+omJGywZkWuhITq9/+2GjxdX6Lfa6jY7IhtcwFivCCLL0SVjMsohE3NnI
                0yv3qm9w1ktQXgofKBcThDy3OcDavA637u52xVbr488K80+eMQfudYHD1R+7
                DcUrCA6yc9FsFFPEeHYsvHSrmIbTR095E/KrSeN4hkE2Bz2H34pU1gg1ndei
                7XJruN92NLXv9dqS6aLuSm/sAZqVLsFIzl2tx5+LVcUs0UBoq2UbJplcE9Xa
                uWGh56zWT3I/7eYVXuvKTFofCnDUs0OOL9WiAChbMw4sLYnuEW1KoqLfGU6c
                xsnAPwKtJ4ZEmBtGbtFPRftMbJxj7magb3niS8JISCuhEhbnKMD3iGhS0no7
                GQYXa2OKQ6LUHLuzSyck1mOQrhgeLTy/XzjdEHj/Tj1m3UwElSGhsbpkac/K
                /rsePsqMDr7jcvdOYPhA47+CwXG70E6rkGME0HDzsoaAl+latIFZalphbNDf
                s3SIk6GI0/HJOVOwrZl5h8/ottQazL7loax3Fz/fE5YWFy9fqMOMIeXbf5+I
                49K/Wl0/K3vfx1EHx6UAvkkR0ZwFZ6u+VT5XtM+cRnYTkzQFDdnH3NNfkhsc
                v2a/Q8LAXwQYAQgACQUCXXJytgIbDAAKCRBVuMlaYnXZgULmCACMWh3kH7b6
                TJmvljtcXTFY7pdzWPZNpecOMSNMZZhTX4PpgMXcwsLuBsHtcyptOnJ8vfCJ
                HFg201tG6cuyg3zPr+RCgZPtFNbDVVr6Faio1No8JAACi9DHEkrpR2kh1s/m
                9735l454HbcXjcZcXK/fRQeD5rL/wfNePIFVWauGESkA4s9Vy/hXtAfXWKHx
                rjcgf72uUbGs9rAxxxS2suW8C8yeQZv5VQA1Lv9IRGGYL4wOUYiiU1d91KlO
                QJgafWQUF30GxUClEl2jLL2t0OqySdCM2agkVx6pZ3SEU8he8IfAh3rpbBYS
                b5BlAx31rUZ+NdCZyPnU+83opOdYrRjy
                =4Q+q
                -----END PGP PRIVATE KEY BLOCK-----
            """.trimIndent(),
            1,
            0,
            """
                -----BEGIN PGP MESSAGE-----
                Version: ProtonMail

                wcBMA7M4YhTWmh7GAQgAwb04y2B+brcyyIrknDy23xQPN8sOyXhUjSOkD4b+
                oS9MrA5jJoh4roeicDGAmfs8YFWBhr/raLG6t2iesxqSZ8zpf52u05DTeJy8
                8nRd+1ACh6hEMYPrMzQeHC+joDjYxRtuOh/kDetQlO/RVVrOmLAlCCxXE+eR
                unr5G7029u7VhaCV7tg1t/b+wYfu8wiQCmZBvDXZVjlHvcCSGWCDdtHYk6sV
                q/rQvnSJgqC+6ilJkk1ujHkaP4Xt4CSHSDOmzHPuYs2rLVLum8U8EEpZVRVe
                plt6P6smMIm6dgfBUhVrJUKYCCn/HVH1aN19saBUVpOJ4MZFJhfxynEgcstT
                ddJ4AYZxxOU2VgdwDePMqYXtDTlL7xi46LW8fJvUitAv6BWpLJhhUR6HsjG7
                oJj0VjFCreBFHXniBPyFCaivX28U+PFgSduHfq+8W5dIn94E3/Bx6Xck/wcp
                y/iDQpC6sVUOvsGeuErACZPuqF6sOzIL+Bva91qhoMDO
                =5QCS
                -----END PGP MESSAGE-----
            """.trimIndent(),
            """
                -----BEGIN PGP SIGNATURE-----
                Version: ProtonMail

                wsBcBAEBCAAGBQJddhY4AAoJEFW4yVpiddmBB1AH/j7O0LnFxKSpPLj2s0Cl
                btmfj1MljLpAu303VxL6pFNxdlTQ0ehrt8Ff76aE8YRXU33zukm8mcHOM31e
                tjrfJhib011TcxACgkKtgs0WPpERhtyxhGpAGcIXzuEdyHIDnBfvbU4sls6j
                4KYNDFBcN3vCk2DiWRRVJlXnF88cffWNq31fpkkfWAaGl5ePFqnEz54mzxm0
                /MlZ+i9LP6bBBpnF6QBAO8DWYnGl3IVT9qjR0Crzp7T1obRbO32gorhU/ifA
                YGyKRSN6axG+6+01a523I+0qnCmhI4bFiGvE7f3BJucB/QZI4wuWLMhAqxGW
                IKS2Qorq1xywg/8FbDNG7GQ=
                =Jrx1
                -----END PGP SIGNATURE-----
            """.trimIndent(),
            null,
            1
        )
    )
    val manyAddressKeysUserKeys = listOf(
        Keys(
            "IB2lsghg5bLlCJkSu7eNPq-PD7Ae9ZXn_yy52sObxxF1NzlM8iuVY-oFvfFFxE4egvWMqse402s_ERB9SbM7HQ==",
            """
                    -----BEGIN PGP PRIVATE KEY BLOCK-----
                    Version: ProtonMail

                    xcMGBF12FxwBCADZPDcFbXjg+utHsg9RCTgUkIdTUbRzxme83u4r5UqxVqIf
                    coUF4at5uKPj1XVXAtpTxEDsJ8RfseGSZHTUxcwkYWUDL0YyynFY79GXYd6e
                    1tAKLzXmlNPHBafKoN2gubXmC2zQMjPvWL8K4Vqg9yhEFaNlKRSnzFN4n6Ih
                    G92Yz8pyid8wBUcbGtJ+REGQpMvSugRkpGzz7p75IGLSkIaMlvf1AnRrUKRr
                    MB8i8UzkpyI8kjr740JmWNqxpJWzczkSy93pjsYB9drvoFBViai98EGa78cT
                    YGK9LjU+brWMu/9Vw7nQQERG48ISePa5bkdSzNACwkjvSC4xISmu4KuBABEB
                    AAH+CQMIG/q0/yK65rhgIih9pNUXykFaBSRd4w6EU5sQ0aWPrt8JHPVi5BqQ
                    3J4Qlrj6GM9n00axD1+jz5jmAvuAcPWx2CGuYZm1a/zBxJtgodHX19ULJ9qQ
                    7mO2TtAEAmnnPzDgnE7dJzoIk3TIGmKhV3vh32Mv2lU9J+43ctU6clgHSqLD
                    J3pIfNW5a8ZIgg7o5sve11frM0lSCf1ewzt8icf2vihcTBmJd0wCiHqPOlo3
                    RIsmXelHSa8ffaRjOy6Yf/vNXPl/LSdn/60iCBXO1fit46Uk7OZcZJeLkE6w
                    Fgr+4wyMGn2iDyc4xsvnZ4asxNw5wUVPxEQ23wz8RP6p/kAOvnzbM+jfiRuS
                    y6/Fzxnve/fnVN9PxQugkIsktgbYuRvCuOcqMSyPZkXTfH50nPP2ITdoCM/X
                    I7CERHTeR2cQinLL82vwp8A6wkmhoIo/9NAuJcGvTelqa6nfSV8egzOteoiH
                    BIiLFvHx1bnvEUG4MUlZX6xNRwjt3kGES08a+iAdDQOeTqAU28YaTHhyjWbo
                    YuQ8gUYPrCs01XzDqi62+BwVTzxXie/t7RFq9WhNeL8BOzEsiLR2LfyNzvaV
                    7/tk8SHJ0W/n+D2tbcUuVUkxWuMH8ewcY+vm3DJquzoxkgtSdd0BWn7WWJIx
                    gN5g2EfTmPfijvCC3W7oU/PE7aQVY0gK9A8OEA8pNIyNdlw3OeuAIn0UdlSC
                    xXHsjcQFnedUn363GUqOJpU04iKyoAO/v2qTb3nHzDYChewTupnT7RZZlkSj
                    dTLmHjnQhOQRCPRo8P6CWRRg2iWJgo2rIQUJxpBV4jop7tZ1ud5nHFH6ZbpY
                    +xldkpA5LTybJl2emMasdXUdi5MwjXKnBOpQLjt8p2+FFhDkl9M+xxrsAabp
                    xJIaiooz9Vdk9bRPQeqxj0REx4cHo0uMzS9hcm9uMjJAcHJvdG9ubWFpbC5i
                    bHVlIDxhcm9uMjJAcHJvdG9ubWFpbC5ibHVlPsLAdQQQAQgAHwUCXXYXHAYL
                    CQcIAwIEFQgKAgMWAgECGQECGwMCHgEACgkQ+Krxzrw0Kbw83wgAivaBBVVk
                    7M3GpMBMWUBWhW/S+V4BdTu8QuKiEufUbHnZCkpqQppE2oSazeg9UY0WRwDk
                    g62q43NGdaGHQpJdeg1VKgqx99+JLnpu6x2AzU4SRpQmt/+SBpwEFEI4ryJP
                    UDwA5AnF1TdmbhOnKSlU9hyRhT1xRT5Tt3eQpM2V3yAlbmUxLliRWbMX94z2
                    +i4Z7KFev3UlPVnaI/+NLgfaWJqTaL8JIygpezCqUaA7nKsrKHLKo4WqIAPv
                    SpdKk2bOqju9B6sQfUBuQudiLQJWKBDqcpXW8G9RHjdk3umezKNsRVKekRGR
                    DyMlDxb+mQHQfYHtXxJutUiFWRm+aUpS2MfDBgRddhccAQgApxoM6zdh6+pr
                    LbH9PfUIitpUfxfj3Yct9749PnTRCNiWvnbcXGr1pZ4mg/DFtbEvX+R4yj20
                    /xOXmIhgZrPsFsK5Di0ug52GiF0D0FosqX/ROJMiT3MisKGV39XUWhgyaiT0
                    dk2FGiJ79UfeELXUncp7mWHI0qqQDzruShdK4ho9dHP0uO7QZwbWcN3dIr63
                    blPZXQ882438rbUJxW1ZlNKq0smqaf3AQkpKkvK/J3feCHrL6AQgHURne0jB
                    Qr1WdVgLRafKLYYroSMCt4eqCYFT1NOkK7H9RWTzHuEHozoUHzr1PGos2vzG
                    euPPU+QAk9l0NauumljPPJNgoFdjiwARAQAB/gkDCFzuRXrswamEYHCtwiEk
                    YU1WzgGEkHIKG5/nTHttHkex0psXkbI3TSWY8/L5c7pONQsVX10d688L1xP3
                    RXNlmkdwRtAoHzorFs2xFC1tsRMG3mCGWvOxUh/o7iBNXb9KITK3cdfA9tn2
                    B/B+41YpIDnBZGvnE2dG2dHmgbhjBVhAi8bbabAJz+Jgl9kDCVWnUu76Bqoc
                    VQrWuDmFKt7Vx0bTl1acQyUgI7fbPN8WBqHGvZS2GPI9dg50YEzX1qseOxxQ
                    ds6uCcIodLoQGOV5haHdjJPwfZ45CPlLyj6AUl+Mg9Y2301A+3w/BMgMGWLt
                    FMuRtDWVUqoqJO4zAsA8LeTfQ/RStKxJ73ZO3OZr05S3jlVq8Sms0fCfPm6H
                    BR8A64hc+4rcTPwfDJ1/udYoU1N3mrElBTcUjeX5/+x91BSHzw8s967Fo9Oj
                    Luu4XuEJvWY10qGKuW4deOdXono6/VgMYPQGHBJoe7NPJLLd9wr4/nFXF8Iw
                    6eM2B+LbLwne6KQ8Rex0yxN5Lpi0j2P8s9+BbYmC9/35Q5AIBfuRSqdX9nUi
                    GjU3goHBFHThsUyR5m0nUdMm/m+vjmeiYd9L+EGfiPG4PFAFvYrmePMrJt+D
                    Bkx6t83oDiX/wP/ZTlLLRAC17QB1HKLrUeClLtcyhZsjYHr2FtfeD9b+nbPY
                    /qpCg1vYjSnAbp2tlAU/jY6caHhBRaO4H7tMBALijRzi7W8QPpV3ri6/YBof
                    1Obfv/BBYPPdkRSPntyExRY5KdrJ57TtSEOW7km+M5baC1emtoupu0/6QubP
                    9R0EuTJBeUmaN4VrGe3f9ielq1RgG00sSNS76vOX3PKmVvKZYlM/uD+scVlN
                    sHbTKf7NXXGn/QuAjjzgqqyCs7HpnuztB9C6y3j6+eqL6waQWCo0eSbq0PbH
                    DdwHwcLAXwQYAQgACQUCXXYXHAIbDAAKCRD4qvHOvDQpvDf4B/4g4wrRhycv
                    m3CmnaDI1F7X0JBYp75TXhpFPm+8Tbze3lg2KLYtpO5Ut1/o0ugcnNLE7y14
                    KR3ZcNbwuGbxrhfeWQYNadkOV2xlIZntrkvqpuMY6XbinYkkcdVfDJo8kZ5q
                    iky+ghv/UF3VRC7PY4E211Ic2d3nI6Rn1YntFpFNSm+VNQ6hleHDVcPNG1vg
                    8VcrgENsFcC/gfmeGgSxDCTIDItHHF1RyRR3x7TJ4Z0qUlN1G647nT6GkO7o
                    fGMsIHl9AHQ0fgmGZO8Og5rbyUxOT/HW3E/SoSG84B2OuaxbLntxMcNS9u9x
                    a2j5Ak+MfeXknuaYqP6hs5BzulMTTJjM
                    =sm1p
                    -----END PGP PRIVATE KEY BLOCK-----
                """.trimIndent(),
            0,
            1,
            null,
            null,
            null,
            1
        ),
        Keys(
            "XFEG7ogZjpsk23o8AIPlzKZiqLnLlATQmbIJzt-RubRpwhNd6F2I29OSJWqwKiq73IQfctWOn9mgJ8J3Av4Xew==",
            """
                    -----BEGIN PGP PRIVATE KEY BLOCK-----
                    Version: ProtonMail

                    xcMGBF1ycrYBCAC0pSbZjrqLzSunLBItB7RZVrQgkvWBP4GClkC2KciKM5DD
                    eELgxIc+OkMKgA38j42hDKsaQBZ77ugbT0GEaRRZeoINCXuna8tZfLMC8yHF
                    ha3aTa16Vh23FJl5tTQDUlU081R+NpST9LjkqIluwayax/WXdXSQUBgVFDuc
                    kg0zCX8U/nqmx6mRnwlE6D30dSqbB0tCS7SjkXEgOLdRpZXqyaexvzziJaGF
                    Rur3GDpJwatnjepJdsrtjT+NppolJycfqbeNe7AodBQaPynQb1sLs+c+NMf2
                    7TpMjOjbmHN/ReW3BZdQDX2JkTFmiDlfvy6v1jzyQ5uPPRi0eqLD7hWfABEB
                    AAH+CQMIgZZ8g8MsEBBg10OUxTrzvWhmcPsUBafeDgSEGU6USTVyKKevfn6Q
                    OLkITEwdLsLH40AJG9lep5tjKqgbcYXwTlK8q62ZcERMmrBHAjrQ/FdceiMJ
                    Z/lKsNd7o6OUrHF5wdT9SBkFKY2fNrhCknX8+yYB1lkW/hbsTdjrFfouqz6g
                    4iAM4qLFenykMHGsR/2w0HyRfNf+LhPYMwY3hs35xeDFlQGJIqMB0KSp8fhL
                    YMCHfJMk1A4DB7CWzoLMHNHB2+j3vEHvRlDdNWASldbiM/Ta1eD3RANK+nRp
                    sXcd19dw+RoO/uN//hYG5m/mut/iPnPfv8ySHjX8M4KICaeAAxKIBGNws4Q4
                    AZvq8vd8rD9xec4XYLAcH47yec0hxG8/vYZIFJnMcsf/s6zPRmydqj8rKmAK
                    JPvBJiH3qbGXTd63lAqIkgjLs/NaxbZltmt93WW68gZN3olZkkWCypVulJgc
                    XHYf7jj+TwFe3IpkdGoKHv15jzPLjfY+dNOnJ2rSmO2RPamCIlJcTH9ctY6q
                    cVikFpMHB8NhKzH8j1pBd7goIcR3ICd6xwESnf18iGKhsM+USHySftUfek8V
                    SN83+qBvvhuTgY67JR7cM/c8a90VXXlWPwYzVqwS2e8DpBbV8yq2fSAh1inh
                    WKBSb8M+We78rc0TnEUNrel6hbqwKcEv6ALHmGwo1g6JjaRsI1GtWEtveZ0l
                    g5PuFHC8Ga+387FijCP+vFdDtkZyvosgtkLQ9uPYGs1YxRKTr97rYaYzzm/O
                    h9thvZmbOA9CynK5c/rwPG0zbmlquiyxOsHRA+e4gaZT+75L5JVyFnI+01vC
                    62ZNMnEFKfm9+GLo/6fU42K+oFEsSR3IDkfxTvTqE+kTAXg6LL1n1jvi5/q2
                    Ot3s05CXn2p4G1YZuBgaBA0q4SEHl4CEzS9hcm9uMjJAcHJvdG9ubWFpbC5i
                    bHVlIDxhcm9uMjJAcHJvdG9ubWFpbC5ibHVlPsLAdQQQAQgAHwUCXXJytgYL
                    CQcIAwIEFQgKAgMWAgECGQECGwMCHgEACgkQVbjJWmJ12YGvKgf/YUqyEui7
                    85dEkdCGUR8ekJyOrdUSBNFsctazWNcVSXSJ809YziM86hoGG/gTmz83PgCn
                    jpXr1DwhklHt20nyx1y9cRyPaw+hBtastknIFsHkUYaBeuLayytuevsUMZiv
                    Nmox/3fyRpv/HLJzM4zzdSlvAnhuKvyInDB7Ut1VtE51dbomIby54Xs5+CW4
                    tqWCB3PWEaW54La+i790dbB6meFuuDAHaR+BeNDYPRT9v+6opeUo2xifwDW6
                    OjNmrLeN9rwhtXWXz4zzRZc6V4VfilbV5dRVDN273Bh0wj7iHQJnwGG1B1S5
                    K1Xh3WQIlo4UFyWE+znlCPalZvgxmxu4OcfDBgRdcnK2AQgAxMCdP4dF2jSf
                    WbVyAJXtgSEfjQyusoU6CRq/jUbSo53no9vJAMOxDHtvJ+54vOm6DY1w1aZn
                    Rnf3chaAlAcE0ZTnyfHxuPZbqs5peyAMtzfQ7FKzlAuV0lMNJ+g0nC+hB8jW
                    E7VWhwzUgLueD89PMFlG+TUqcwamaYXk/AOL+IIVbOC7nUXDkDuT091D6fJq
                    hff52moElSdkSEnMTKAWONhrYrutB1N3V2v1WOASjMncorWzksl+pvk3jzOw
                    /qlsHxRi4ZBFAkW3K2Sigyx7P98Jd3bth7zs9H9YMpX8y0z0UOR5bPjBqsdo
                    Bh/4wFo+DNv7SmUpMf0jouW3uxQbgQARAQAB/gkDCFTlQoRLVndvYPQf8w7Z
                    v60GvupldAqeiiO17ZliaNxCXbXjIv2eNMSeMemJkdh8hMO/IoCDa3FoW6/b
                    FPPXcbSF1Wsgn0YJRRAIGbtRQR+Oa9i4X5uY8S+jRvBmOdcHOvDH+8yYKyP6
                    wMJoSDRTESHzNJd3DisacPijAIfCtj5J8otrQFX/kxUn1f3EdnwA1XZ6kkL+
                    FNBARJtxmPMMJhmTEooTf/0ilBlIK+ANpWUjL1jh2Tg+WCU5679dFcL62FSP
                    tQtHQfQ+dt7yaNxUNat99QBMpvUuN1XpohdYuNsGv7Qvy5UWviq0I5lHW9MX
                    +hPubDwJQkw6jojzsSxW2iGWjNrvTd9/6kVcwRZVKmADGW7NWoYoQsUcR5hB
                    HNiyy4EAnU21FRpqiXg3ClkqK7eGCUskkrhplJkWKZdU+AfAZBASmARyOVt8
                    Kcylig86owDqrEE4QPOqHMX+/H7zbtixKF4HUxhsGRncyF9DQNArwkY7cThA
                    caj8wYJUl9ld55JhrMttfYuVm+7/9Q8aROY4jyhJ9b+ziNjUYKCKVhHO03/+
                    mAuUy+GxvWfkXREeQxMbP0ZQdH90yLgFZICMOCT/i52pEKoK09EJa79f5c6V
                    NYaz5TYCFdu6mEMwrKTTgWpkfYT5BqwTX3nFGqQ1PpCf2I49LvudBF0V7fMu
                    9S5pYIujKbt/InPupdSJXZ5ePseCS2AG23xyLXGwXyVUm9fF1CVqUfgKqm7e
                    5hTtIneBX8y6uv6/rzGUVA0OdmNTt/gvTsAKxLsaOHmdEVszjpamsyELH01I
                    muG49Ra+6dh/DUy7aYqDYx0FMX7nNdqOfC5ayHH1D7sZ/JSFyoJOltsKmVik
                    /TjiS88CGkhn0fX6IHr2StjBf7nT0KoI/KqqCe7/J2EbW0meK6zKpwCx+PmG
                    egOqDsLAXwQYAQgACQUCXXJytgIbDAAKCRBVuMlaYnXZgULmCACMWh3kH7b6
                    TJmvljtcXTFY7pdzWPZNpecOMSNMZZhTX4PpgMXcwsLuBsHtcyptOnJ8vfCJ
                    HFg201tG6cuyg3zPr+RCgZPtFNbDVVr6Faio1No8JAACi9DHEkrpR2kh1s/m
                    9735l454HbcXjcZcXK/fRQeD5rL/wfNePIFVWauGESkA4s9Vy/hXtAfXWKHx
                    rjcgf72uUbGs9rAxxxS2suW8C8yeQZv5VQA1Lv9IRGGYL4wOUYiiU1d91KlO
                    QJgafWQUF30GxUClEl2jLL2t0OqySdCM2agkVx6pZ3SEU8he8IfAh3rpbBYS
                    b5BlAx31rUZ+NdCZyPnU+83opOdYrRjy
                    =kArG
                    -----END PGP PRIVATE KEY BLOCK-----
                """.trimIndent(),
            0,
            0,
            null,
            null,
            null,
            1
        )
    )
    //endregion

    //region Token and Signature generation
    private val tokenAndSignatureUserId = Id("token_and_sign")
    private val passphrase = "7NgO4d0h72zt4XuFLOUbg352vhrn.tu".toByteArray()
    private val randomTokenString = "9efb6173a8da137e7ead1a9d2b6ada0f707a19156dee0c84899761fee73e556a"
    private val randomToken =
        randomTokenString.chunked(2).map {
            it.toInt(16).toByte()
        }.toByteArray()
    //endregion

    private val addressKeyMapper = AddressKeyBridgeMapper()
    private val addressKeysMapper = AddressKeysBridgeMapper(addressKeyMapper)
    private val userKeyMapper = UserKeyBridgeMapper()

    private val oneKeyUserMock: User = mockk {
        every { toNewUser() } returns mockk {
            every { addresses } returns mockk {
                every { findBy(Id(oneAddressKeyAddressId)) } answers { addresses[1] }
                every { addresses } returns mapOf(
                    1 to mockk {
                        every { keys } returns mockk {
                            every { keys } returns oneAddressKeyAddressKeys.map(addressKeyMapper) { it.toNewModel() }
                        }
                    }
                )
            }
            every { keys } returns mockk {
                every { keys } returns oneAddressKeyUserKeys.map(userKeyMapper) { it.toNewModel() }
            }
        }
    }

    private val manyAddressKeysUserMock: User = mockk {
        every { toNewUser() } returns mockk {
            every { addresses } returns mockk {
                every { findBy(Id(manyAddressKeysAddressId)) } answers { addresses[1] }
                every { addresses } returns mapOf(
                    1 to mockk {
                        every { keys } returns mockk {
                            every { primaryKey } returns addressKeysMapper { manyAddressKeysAddressKeys.toNewModel() }.primaryKey
                            every { keys } returns manyAddressKeysAddressKeys.map(addressKeyMapper) { it.toNewModel() }
                        }
                    }
                )
            }
            every { keys } returns mockk {
                every { keys } returns manyAddressKeysUserKeys.map(userKeyMapper) { it.toNewModel() }
            }
        }
    }

    init {
        mockkStatic(TextUtils::class)

        every { userManagerMock.openPgp } returns openPgp

        // one address key
        every { oneKeyUserMock.keys } returns oneAddressKeyUserKeys
        every { oneKeyUserMock.getAddressById(oneAddressKeyAddressId) } returns mockk {
            every { keys } returns oneAddressKeyAddressKeys
        }
        every { userManagerMock.getLegacyUserBlocking(oneAddressKeyUserId) } returns oneKeyUserMock
        every { userManagerMock.getMailboxPassword(oneAddressKeyUserId) } returns oneAddressKeyMailboxPassword.toByteArray()

        // many address keys
        every { manyAddressKeysUserMock.keys } returns manyAddressKeysUserKeys
        every { manyAddressKeysUserMock.getAddressById(manyAddressKeysAddressId) } returns mockk {
            every { keys } returns manyAddressKeysAddressKeys
        }
        every { userManagerMock.getLegacyUserBlocking(manyAddressKeysUserId) } returns manyAddressKeysUserMock
        every { userManagerMock.getMailboxPassword(manyAddressKeysUserId) } returns manyAddressKeysMailboxPassword.toByteArray()

        // token and signature generation
        every { userManagerMock.currentUserId } returns tokenAndSignatureUserId
        every { userManagerMock.requireCurrentUserId() } returns tokenAndSignatureUserId
        coEvery { userManagerMock.getTokenManager(tokenAndSignatureUserId) } returns tokenManagerMock
        every { userManagerMock.getMailboxPassword(tokenAndSignatureUserId) } returns passphrase
        every { openPgpMock.randomToken() } returns randomToken
    }

    @Test
    fun get_key_fingerprint() {
        assertEquals(
            oneAddressKeyUserKeyFingerprint,
            openPgp.getFingerprint(oneAddressKeyAddressKeys.first().privateKey)
        )
    }

    @Test
    fun decrypt_message_for_only_address_key() {

        val encryptedMessage = """
            -----BEGIN PGP MESSAGE-----
            Version: ProtonMail

            wcBMA5kajsUECZmgAQgAgJuGP/0+pUPu24mWeviRQ79s6fKKsKh6y1aBXwJM
            eQ8mSaLvHNSaCa8s9yozs9gWo2/Uf8Lpmqb70SMh2npwI5hyOFqXsrMEoEHn
            KTf86kSHnGZEtwrScXnekJjO1rfYynnAYuppTfpUc2E/uGZg6RChlwPbBZMw
            tOk8n6iL6u0+Ren9fxAmmMTw66vc5PDejmfAgzbdxeD7qV8wzqmipgiErk/w
            dPEzI5QGtGXUwsDfJeSGEdCslN1kHtZRj2B3tg6Ms7Ea/VIb3Kq6uyn2hQhS
            MlWwjzauF5mryV4Kbi1RP6yTykbPnRz6ia22HwbWzOVJ2Nu534RqNYA/99Bd
            G9JcAXjM6al21XdX0ZQww2R0Of3VzFVwQX+RSG1SWGq11u2nu5iXBVUJDa5x
            MS2SksqmW3Bh7Tbz2zlrCNZxH8USiAxXt/3xjwNlXgCg4b8sKNHNN4+Wa6S8
            HNwbYAc=
            =9RxF
            -----END PGP MESSAGE-----
        """.trimIndent()

        val expected = "Test PGP/MIME Message\r\n\r\n\r\n"

        val addressCrypto = Crypto.forAddress(userManagerMock, oneAddressKeyUserId, Id(oneAddressKeyAddressId))
        val result = addressCrypto.decrypt(CipherText(encryptedMessage)).decryptedData

        assertEquals(expected, result)
    }

    @Test
    fun encrypt_and_decrypt_message() {
        val message = "Text to encrypt and decrypt."

        val userCrypto = Crypto.forUser(userManagerMock, oneAddressKeyUserId)
        val encrypted = userCrypto.encrypt(message, false)
        val decrypted = userCrypto.decrypt(encrypted)

        // TODO this assert fails because we probably incorrectly handle SignatureVerificationError
        //  in TextDecryptionResult(hasSignature), or Helper.decryptExplicitVerify returns incorrect
        //  value, 2 instead of 1 when there is no signature
        // assertFalse(decrypted.hasSignature())
        assertFalse(decrypted.isSignatureValid)
        assertEquals(message, decrypted.decryptedData)
    }

    @Test
    fun encrypt_and_decrypt_signed_message() {
        val message = "Text to encrypt and decrypt, signed."

        val userCrypto = Crypto.forUser(userManagerMock, oneAddressKeyUserId)
        val encryptedAndSigned = userCrypto.encrypt(message, true)
        val decrypted = userCrypto.decrypt(encryptedAndSigned)

        assert(decrypted.hasSignature())
        assert(decrypted.isSignatureValid)
        assertEquals(message, decrypted.decryptedData)
    }

    @Test
    fun decrypt_message_for_non_first_address_key() {

        val encryptedMessage = """
            -----BEGIN PGP MESSAGE-----
            Version: ProtonMail

            wcBMA7M4YhTWmh7GAQgAxAdgbJWi7MKSMiMg5rOUu6Y6nFJK9pgU5MsrKYqO
            /hXkkpocWTs4BDL+AXmy86e0C52mwsKJj/cFZ88erFLGMrkG+sVkDFi3fZ7Q
            dqrqKrzbGg6NubQpCtwGv+KvtFcMfCUWD4jeH/saD4wW9ZAH3Ozu0s/VamIX
            62VDi+l6TrZIUwsC6Pnyy5O8O1BnOultCjUP4bYApSfQIBDENBVyMVT9pp1/
            ylfgUSZQCj2vWkbMtMH+SAgBgk+MMYVBTx+Pk1O9lhZdqXhjzEmi58AZMdq7
            /+CGjJwnySBFCLaHddYfzvVVQEAJngRRl7WA+CVkskMc94w1nwlVeuARuAiy
            /9LAaQHHE3Wb1en/rqK4IPK0qWaInpVualn6KeORmtnS3Kl2Xynt92Lcckoj
            37WEdjXDCIhl4JyrldelRmaxBisnW3te4vsukGh87E4jL8oDvIMwHN0bm7KH
            +kBnlxqrR6N5vZmcjFoU+n9XBYDkoPZ0MZCwCgMi2BbWrQv7zy/o3+35kgms
            6c3Mwb7nIP15ksysLz884tF6k5cVoLFISL7OMqem1uKM66BgOYJovvRR1Y+v
            70aQ/G2w7B44mzPBOlzOAzhDQDHtxNft1XT+LH2cjrExd0EzYE+8fpYpOHC0
            KfHrt6wx/sj/O+e1M9F19UGDIJMFRmlgRIUJCEmpiaZnWjmdajfUpOPd47ac
            GYmSnoyEOzjf1Lyy0M6w7BHPQgwaF7Ss94EAcsFcfw==
            =iJT4
            -----END PGP MESSAGE-----
        """.trimIndent()

        val expected = "Dear valued customer,\r\n\r\n" +
            "Thank you for subscribing to ProtonMail! Your support will enable us to scale up ProtonMail and continue our mission to bring easy-to-use, encrypted email to people everywhere.\r\n\r\n" +
            "Thank you,\r\n\r\n" +
            "The ProtonMail Team\r\n"

        val addressCrypto =
            Crypto.forAddress(userManagerMock, manyAddressKeysUserId, Id(manyAddressKeysAddressId))
        val result = addressCrypto.decrypt(CipherText(encryptedMessage)).decryptedData

        assertEquals(expected, result)
    }

    @Test
    fun decrypt_pgp_mime_message_body_only() {
        val encryptedMessage = """
            -----BEGIN PGP MESSAGE-----
            Version: ProtonMail

            wcBMA5kajsUECZmgAQf/ZPNo2ycCcSfoHJpRvHBpaxDOhj+pJmynsIwptekE
            ygoiPTUhLRtVCGSoAZOBzWYGU2wxWUAUZupJx3enkY9GGUTNFYxNgoWIjSqZ
            G0BmASdgQBLGfINjNACdafiFtm1to2z4s41Vwv9ENBornkABzR7VqbrSe5EY
            fef+ZwtXDzg5lyO6G4HqOMHmrX+H8gbDKslS3OgHk66+e58HsWPRDK9DvUfz
            YqGAybRZpMwAs0UXhr9MXrnUBzzd010NGo58iWugt4zggyO/xfiMaC4cXLrm
            2LmGlTQrJ8+gNUSTPcOqpDtovS/Ve0LbFAcXjLMZsxyTx5UwQSFTgDZiaQwz
            gcHBTANv03LIBj82fgEQAIC2NiDaRLanLPwlDZx12YbrgDKcjywDWTdJDg3X
            ABmKK2RlnMpK8xV3ffRN+MHQv6o1a1kz8U+l5PZx1NxFwEUPEFzCgU43hg/t
            qgWkZDs20BfPEv84uwnxB0UKvN0BqUorCRfIqrGmcAgj/9ZcbQqp9xUGk3Wo
            rM3CbgCDjkL6uu9USpPdiT+sVx/I4BatZQFBBGUn6IBkTGA4jivBmlCOyoed
            q0IKXLL/HhcdaiicY3EgVkSVOmzv4Qvl5rS2NhqUuuzhm7J/Q0pBTjD09MuB
            ypFpxZ3SChTdCvYrSzXSdTHV0A3WzmjcWAPxFBQeu6EyAKDFY8xlbIGmJcuw
            x1xd36IZ3Lz9wvHjVkK6AGUwLX4Gbr4LqI+cv0737Kylsn8nZqNYzYyu1pIJ
            SRtOigIERnQQ7tJUdB9MPCQYV1HNoqtERyIsWsYxHxxIBjyh08574IbRFov0
            SYvBGyeiWuUbxIWIYKNRloezrr1VdO3Ir2NXpOQcg9X5bLmFcyWLb+HkmP0L
            IoqPip2oj8p9bK3W5eugGpuB0Wg4rtLMC/y4uGagZWQEuIfDwbaU1DJY53Gd
            gLOrrwugxokWpff46nsJm7fDuKtS9JRRSrIf0kEK9U9r2PiZJ5EZGk6Xlfls
            rf+/B//B6CraAxchCuBKZWfiSkwzlp5JLNCHoJ9ClfiQ0sDMAUuVdfoLxykG
            pEgYd7WZWhL/HWqL6wnJwIgLWc0KNlNfLTe3xDzsrH40TrggeLkhxt5SMt+/
            Z1qHe1I2ySAG71T+h9nXQvf59/TCJCrYeovONMytFWWM86IAlWdaGUf3UgQJ
            2nbRBA1mjdlDXLASnR026jQZCGLvwI/AtEtwf/rFZCr+9BHRp3xqL/7N5eAM
            rQkeVtR4GHQ/5b2k/nAgikHSwPZVq3rWW7zq6LZov5o+bWrpkTC/jADoPJhS
            17culs74jO0+uJIDvrXjs15/2wy8gCorjP1+uVo9jSwEl7R0YXe3N1Dk3GLJ
            qfXCH7M6YagGA+QrKPGVbf+u9mzW8Xa0a0LkiztOQAjVeIATQchc2CULuDaQ
            3GQI5plpG+UXk56hKaHaWqOW4DFUwqytfpUouqC8HYtkNK/U/qof7W7XyK0a
            IQDm/u9BGpUTGi0RRIKw3MceOaqhx4CsVY+4KI8TZxTBUgz0FEolsSGc3n4z
            b84EjTEttONJFdRubt7KSWkoibrlQ7BBLfkd
            =47eC
            -----END PGP MESSAGE-----
        """.trimIndent()

        val expected = "Test PGP/MIME Message\r\n\r\n\r\n"

        val addressCrypto = Crypto.forAddress(userManagerMock, oneAddressKeyUserId, Id(oneAddressKeyAddressId))
        val decryptor = addressCrypto.decryptMime(CipherText(encryptedMessage))

        lateinit var resultBody: String
        decryptor.onBody = { eventBody: String, _ ->
            resultBody = eventBody
        }

        decryptor.start()
        decryptor.await()

        assertEquals(expected, resultBody)
    }

    @Test
    fun decrypt_pgp_mime_message_with_attachments() {
        //region Encrypted Message
        val encryptedMessage = """
            -----BEGIN PGP MESSAGE-----
            Version: ProtonMail

            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
            dV+tX2YY9up0zUtDhs54Kb9IiojIiQEeOYma4YN5TSMD0r2MyKdgi7YAxsGp
            SobKHfov8F2VzRxrB7Ys1D8SefcqZGiXF+aqw/r1r4Vl0t4xcZb2xdIsvGU4
            9vbOdrP4HTz28FA7cuuVMIJlm47faq1z/NEQq6gp8kyy/62cM1SClnqy+RGf
            Y//tTL3DpAIFKHZIo4hyNNwtoXCCRn4iOfDtKcQCQGEvhz4nADr5gXgNNd9P
            HfPT/eovNSX+aoae5w35K7bWE8HuI0nWj9xBOPvLmJM7ZNDsma+qCvYBlZM4
            iF66EXRkOonqX129HV/YmMO2j87CznnbeG9kGdosW0UdKNVgOq8fdajlvg4r
            RrvHKyHU1xqfCocrp/8u+6BR48ghee/0oBI0nSpi0hA2BHKfN2NdpTIwq7l5
            G36zwii+NeLEupGbt9zfhFLWdxgEzU81Srp3WUl0SLNZfeHmYGTLPjxKE5bi
            Aj669zwKIdRXvc/wnAE0ja0mTJkwdrvFgr5lqohBd9I9uO81Gl1TvSYzWGV+
            tKDsBrlhi7Iv8pMGIOBy7jKEa3fbY+tU8TlX4nNeCRaVWq36RRuc8xaRTMHg
            ey3zflKFPbXGj+r8P/GJD8MLJyPLkXYmy0tendPgr+LB+8MbkEHDcNn2D5CC
            ExqCJzUPDAkfjbc30+bQaggiTdqVKIrnt4nJF0ORIeev7W8RQ5VfExy/nhIM
            a6yMEt/7pvagPW1UAU2z7qKA2cgH8juUyxjDEuN0bGdVYGxlH0df3TpuxvVG
            DCoEI4i3R9gVQE5P7+bDnE1UjY4rpZGH7mG8LjKi3L1hhifvhYx0S81RqwyU
            K2VC35idOtE15wERB3DRbH9c+HbGu++bMe3t/rgHbemZ7FSSeXOSE8s8L8WV
            M1CskpavejzvYRCTMXdWLxoO9VbNq8WWgoJ7H7rupYLnQeO7Sy3fPu8qt4HO
            01A6yjw1wsRFfyym8aND0Gt/61op35AA+c+IHM6A6k6loKKPZHSBxG5HUIuK
            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
            OAUBTdhDjCoqYxx48+D3+Q==
            =X3FI
            -----END PGP MESSAGE-----
        """.trimIndent()
        //endregion

        val expectedBody = "Test PGP/MIME Message with attachment\r\n\r\n\r\n"

        val mockedMessageId = "0-KKFldtlrtWQRGmOQR1V4QVsnqq7nuHw_nkmdDAd2xtIvibEqnV0IYVS3FfX-RT8BruIrL35HQ35rQkP6VbBg=="
        val mockedAttachments = listOf<Attachment>(
            mockk {
                every { attachmentId } returns "PGPAttachment/0-KKFldtlrtWQRGmOQR1V4QVsnqq7nuHw_nkmdDAd2xtIvibEqnV0IYVS3FfX-RT8BruIrL35HQ35rQkP6VbBg==/dbfcdb13e07ef2fcf26249fec3a4bae2/0"
                every { fileName } returns "default"
                every { mimeType } returns "text/rfc822-headers; protected-headers=\"v1\""
                every { fileSize } returns 48
                every { messageId } returns mockedMessageId
                //every { headers } returns
            },
            mockk {
                every { attachmentId } returns "PGPAttachment/0-KKFldtlrtWQRGmOQR1V4QVsnqq7nuHw_nkmdDAd2xtIvibEqnV0IYVS3FfX-RT8BruIrL35HQ35rQkP6VbBg==/778b4f3c8e74a652aefbee588e67421a/1"
                every { fileName } returns "elon.jpg"
                every { mimeType } returns "image/jpeg; name=\"elon.jpg\""
                every { fileSize } returns 27723
                every { messageId } returns mockedMessageId
                //every { headers } returns
            }
        )

        val addressCrypto = Crypto.forAddress(userManagerMock, oneAddressKeyUserId, Id(oneAddressKeyAddressId))
        val decryptor = addressCrypto.decryptMime(CipherText(encryptedMessage))

        lateinit var resultBody: String
        decryptor.onBody = { eventBody: String, _ ->
            resultBody = eventBody
        }
        val attachments = mutableListOf<Attachment>()
        decryptor.onAttachment = { headers: InternetHeaders, content: ByteArray ->
            attachments.add(Attachment.fromMimeAttachment(content, headers, mockedMessageId, attachments.size))
        }

        decryptor.start()
        decryptor.await()

        attachments.forEachIndexed { i, attachment ->
            val expected = mockedAttachments[i]
            assertEquals(expected.attachmentId, attachment.attachmentId)
            assertEquals(expected.fileName, attachment.fileName)
            assertEquals(expected.mimeType, attachment.mimeType)
            assertEquals(expected.fileSize, attachment.fileSize)
            assertEquals(expected.messageId, attachment.messageId)
            assert(attachment.isPGPAttachment)
        }

        assertEquals(expectedBody, resultBody)
        assertEquals(attachments.size, mockedAttachments.size)
    }

    @Test
    fun check_key_passphrase() {
        assertTrue(openPgp.checkPassphrase(oneAddressKeyAddressKeys[0].privateKey, oneAddressKeyMailboxPassword.toByteArray()))
        assertFalse(openPgp.checkPassphrase(oneAddressKeyAddressKeys[0].privateKey, "incorrect key password".toByteArray()))
    }

    @Test
    fun get_public_key() {

        val expected = """
            -----BEGIN PGP PUBLIC KEY BLOCK-----
            Version: GopenPGP 0.0.1 (ddacebe0)
            Comment: https://gopenpgp.org

            xsBNBF1BfxUBCADUpiiG3AhQK08E2nBmQ50XeztOWArmknINQV41pqGFW5VQkfbQ
            3FYsANhLGqbDBQ0XxmocjKL7W7W8Y4xmHCGgkCUy6gAqGbi+sXY9Sl8xqQNHuZDh
            WVdqT8+Rtv+DRxp/XrGkzC1U8CBYUmmKS92ldy0/zZIvgQXT6t5Q+v+BeUSv4jCs
            nY3BE0UBOljtrTXlOcXRZHQxORWG+kon0qgcJERdwwzhxY6eT8jEfAfJY0hzQaYg
            +6bj6ZR0zkMtY2Psq2M05kzEw4On/dezZETAu1e9fSqfk1mp+H6BeLJ9RUyrFK/P
            qIO48+pU8CmAvTdx5eIihyOM16CFg/3GgV85ABEBAAHNMWFkYW10c3RAcHJvdG9u
            bWFpbC5ibHVlIDxhZGFtdHN0QHByb3Rvbm1haWwuYmx1ZT7CwGgEEwEIABwFAl1B
            fxUJEBHDHo5eB/TQAhsDAhkBAgsJAhUIAADHoggACDYDZkyTMZX69k9uoAygAQ75
            2kb52r0L3dSLge+hUelxJOiVUznbavzVhzjzF2FucXP0csOSJygHNejjS62BDtsX
            iIoPiVDO1+Hr72X4gE277VeZ1b6VozJvKC0+H4fhg/EtkD07oVhHJRxOOVlgUXGK
            nP2lz2ojny0McggvN+Nk8ILqt6ImlvEk6CnTs9XdmcmosMiQU+U+THKrKZ+5Yec8
            4kzlHG8ee7Tim2yn9n/FuBStrYkTJUsDuAL/LOfF9DnzTzukK6kqpDB6kDfMeYQJ
            Lq+Tu642n74P0lqOO0Wy7imI/hxM1W8yqcNdafS7PCuGHD99mecdKWVeYHCCY87A
            TQRdQX8VAQgAyAIj6ytLB1CUinP6f6aVKlLSM9e9Figv1CAw53NHeQBbmwzU5bZn
            tE6UERnvJy6ul7hJr9Jop+O1/jA6zaGanF5wv0nEvTHcoYRpJ4QiJgiQxvhOdItH
            29+jBV1F44xOzlGnEzFAv7GbPecKHAsQgX9qYCj+5ydcttQ29gWQ6nN23G03R3Lb
            KRS9H2uw1SIYGgif8FgKpJemwJjuSibyViXTf3JC8ZUtYbq+vIXqATFFtbrUHfKM
            AKlHo0uLYGq1rRINGR6Dmhu6bGhZonuW0na4+5Wh86kg9c/YI7jSIIspRRkH+v7+
            RXH51h8Rbc2Tiv64qy7cIJIH0Bk0lFAaIQARAQABwsBfBBgBCAATBQJdQX8VCRAR
            wx6OXgf00AIbDAAAgvAIAGyLaHYTjiXG/ORIIAgdQhKBYOozrOS4EcOPNdMSPBwT
            6P+BpNS/GD33pzANVKM9Mft8+NnePUFOR2f4QJrQ1VvSj9ko8P2sX7IAe7+rG614
            LQfzjG5R16KlSVOMFW0K2L8ZxumDdYl/N0BhgtZmB1lg1xY2TPHoDetznMnHG8sL
            6u6vyhGl5a6qcW2g1urlF0VF/CEqg1lwAKhFHIFiNR+X6jCjg0KJa9MjAW6oICOx
            oX0jp195mWix6suRJSWVK14uieT6uL5yYC5tZMz+t9rs7YxCkHxFRT1H5ZLHUD/r
            93liqW+pzUx+bVdz5qNMb0ZonHZRLe3/Fzb19x8UMPc=
            =6gp8
            -----END PGP PUBLIC KEY BLOCK-----
        """.trimIndent()

        val armoredKey = Armor.armorKey(openPgp.getPublicKey(oneAddressKeyAddressKeys[0].privateKey))

        assertEquals(expected, armoredKey)
    }

    @Test
    fun generate_token_and_signature_private_user() {
        val (token, signature) =
            GenerateTokenAndSignature(userManagerMock, openPgpMock).invoke(null)
        val testMessage = newPGPMessageFromArmored(token)
        val testKey = newKeyFromArmored(armoredPrivateKey)
        val unlocked = testKey.unlock(passphrase)
        val verificationKeyRing = newKeyRing(unlocked)
        val decryptedTokenPlainMessage = verificationKeyRing.decrypt(testMessage, null, 0)
        val decryptedTokenString = decryptedTokenPlainMessage.string
        assertEquals(randomTokenString, decryptedTokenString)
        val armoredSignature = newPGPSignatureFromArmored(signature)

        verificationKeyRing.verifyDetached(decryptedTokenPlainMessage, armoredSignature, com.proton.gopenpgp.crypto.Crypto.getUnixTime())
    }

    @Test
    @Ignore("Pending implementation")
    fun generate_token_and_signature_org() {
    }
}
