#include "client/name_dialog.h"
#include "ui_name_dialog.h"

DialogName::DialogName(QWidget * parent) noexcept
    : QDialog{parent}, ui{new Ui::DialogName} {
    ui->setupUi(this);
    connect(
        ui->okButton, &QPushButton::clicked, this,
        &DialogName::onButtonClicked);
}

DialogName::~DialogName() noexcept {
    delete ui;
}

void DialogName::onButtonClicked() noexcept {
    auto name = ui->lineEdit->text().trimmed();
    if (name.isEmpty())
        ui->labelStatus->setText("Name must not be empty");
    else {
        emit returnName(name);
        close();
    }
}
