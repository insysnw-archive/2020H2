#include "dhcp/timer.h"

#include <signal.h>
#include <cstring>

#include "dhcp/log.h"

namespace dhcp {

namespace details {

void posixTimerStop(sigval value) {
    auto notifier = reinterpret_cast<Notifier *>(value.sival_ptr);
    if (notifier != nullptr)
        notifier->notify();
}

}  // namespace details

Notifier::Notifier() noexcept : mTimer{nullptr} {}

void Notifier::setTimer(ITimerAccessor * timer) noexcept {
    std::lock_guard lock{mMutex};
    mTimer = timer;
}

void Notifier::notify() noexcept {
    std::lock_guard lock{mMutex};
    if (mTimer)
        mTimer->onTimer();
}

void Timer::createTimer() noexcept {
    mNotifier = std::make_unique<Notifier>();
    mNotifier->setTimer(this);

    sigevent event;
    std::memset(&event, 0, sizeof(event));
    event.sigev_notify = SIGEV_THREAD;
    event.sigev_notify_function = details::posixTimerStop;
    event.sigev_value.sival_ptr = reinterpret_cast<void *>(mNotifier.get());

    if (timer_create(CLOCK_MONOTONIC, &event, &mPosixTimer) < 0)
        log("Cannot create timer", LogType::ERRNO);
}

Timer::Timer() noexcept {
    createTimer();
}

Timer::Timer(CallbackType callback) noexcept : Timer() {
    mCallback = callback;
}

Timer::Timer(Timer && other) noexcept : Timer() {
    assign(std::move(other));
}

Timer::~Timer() noexcept {
    timerDelete();
}

void Timer::setCallback(CallbackType callback) noexcept {
    std::lock_guard lock{mMutex};
    mCallback = callback;
}

void Timer::start(size_t sec) noexcept {
    itimerspec timespec;
    std::memset(&timespec, 0, sizeof(timespec));
    timespec.it_value.tv_sec = sec;

    if (timer_settime(mPosixTimer, 0, &timespec, nullptr) < 0)
        log("timer_settime", LogType::ERRNO);
}

time_t Timer::stop() noexcept {
    auto time = remainingTime();
    start(0);
    return time;
}

time_t Timer::remainingTime() const noexcept {
    itimerspec timespec;
    timer_gettime(mPosixTimer, &timespec);

    // round up
    auto sec = timespec.it_value.tv_sec;
    if (timespec.it_value.tv_nsec != 0)
        sec += 1;

    return sec;
}

void Timer::onTimer() noexcept {
    std::lock_guard lock{mMutex};
    if (mCallback)
        mCallback();
}

Timer & Timer::operator=(Timer && other) noexcept {
    timerDelete();
    assign(std::move(other));
    return *this;
}

void Timer::timerDelete() noexcept {
    if (mPosixTimer) {
        timer_delete(mPosixTimer);
        mPosixTimer = nullptr;
    }
}

void Timer::assign(Timer && other) noexcept {
    std::unique_lock l1{mMutex, std::defer_lock};
    std::unique_lock l2{other.mMutex, std::defer_lock};
    std::lock(l1, l2);

    mNotifier = std::move(other.mNotifier);
    mNotifier->setTimer(this);

    mPosixTimer = std::move(other.mPosixTimer);
    other.mPosixTimer = nullptr;
    other.mCallback = nullptr;
}

}  // namespace dhcp
