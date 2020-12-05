#pragma once

#include <QWidget>

struct Message;

namespace Ui {
class MessageWidget;
}

class MessageWidget : public QWidget {
    Q_OBJECT

public:
    explicit MessageWidget(const Message & message, QWidget * parent = nullptr);

    ~MessageWidget();

private:
    Ui::MessageWidget * ui;
};
