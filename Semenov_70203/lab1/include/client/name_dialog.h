#pragma once

#include <QDialog>

namespace Ui {
class DialogName;
}

class DialogName : public QDialog {
    Q_OBJECT

public:
    explicit DialogName(QWidget * parent = nullptr) noexcept;

    ~DialogName() noexcept;

private slots:
    void onButtonClicked() noexcept;

signals:
    void returnName(QString);

private:
    Ui::DialogName * ui;
};
