#include "manual_control.h"

ManualControl::ManualControl() noexcept : mWorking{false} {}

void ManualControl::join() noexcept {}

void ManualControl::start() noexcept {
    if (!mWorking.exchange(true))
        onStart();
}

void ManualControl::stop() noexcept {
    if (mWorking.exchange(false))
        onStop();
};

bool ManualControl::isWorking() const noexcept {
    return mWorking;
}

bool ManualControl::isStopped() const noexcept {
    return !mWorking;
}

void ManualControl::onStart() noexcept {}

void ManualControl::onStop() noexcept {}
