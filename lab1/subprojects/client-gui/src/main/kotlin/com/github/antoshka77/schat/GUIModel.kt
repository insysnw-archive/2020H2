package com.github.antoshka77.schat

import javafx.beans.property.SimpleStringProperty
import tornadofx.ItemViewModel
import tornadofx.getValue

class LoginModel {
    val uriProperty = SimpleStringProperty("localhost")
    val uri: String by uriProperty
}

class LoginViewModel : ItemViewModel<LoginModel>(LoginModel()) {
    val uri = bind(LoginModel::uriProperty)
}
