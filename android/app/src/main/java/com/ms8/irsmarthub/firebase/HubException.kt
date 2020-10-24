package com.ms8.irsmarthub.firebase

import com.ms8.irsmarthub.R

open class HubException(val titleID : Int, val messageID : Int, message : String? = "") : java.lang.Exception(message)
class HubOverflowException(message : String? = "") : HubException(
    R.string.err_overflow_title,
    R.string.err_overflow_desc,
    message)
class HubTimeoutException(message : String? = "") : HubException(
    R.string.err_timeout_title,
    R.string.err_timeout_desc,
    message)
class HubUnknownException(message : String? = "") : HubException(
    R.string.err_unknown_title,
    R.string.err_unknown_desc,
    message)