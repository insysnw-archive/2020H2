fun ByteArray.getMsg(): String = String(this.drop(1).filter { it != 0.toByte() }.toByteArray())

