#include "client/message_widget.h"
#include "ui_message_widget.h"

#include "message.h"

#include <QDateTime>
#include <QString>

MessageWidget::MessageWidget(const Message & message, QWidget * parent)
    : QWidget{parent}, ui{new Ui::MessageWidget} {
    ui->setupUi(this);

    auto time = QDateTime::fromTime_t(message.datetime).toLocalTime();
    auto text = QString::fromStdString(message.text);
    auto newLinesNumber = text.count('\n');
    constexpr auto maxLinesNumber = 20;

    if (newLinesNumber > maxLinesNumber)
        newLinesNumber = maxLinesNumber;

    ui->textLabel->setPlainText(text);
    ui->textLabel->setFixedHeight(
        ui->textLabel->fontMetrics().height() * (newLinesNumber + 2));

    ui->authorLabel->setText(QString::fromStdString(message.author));
    ui->dateLabel->setText(time.toString("hh.mm"));
}

MessageWidget::~MessageWidget() {
    delete ui;
}
