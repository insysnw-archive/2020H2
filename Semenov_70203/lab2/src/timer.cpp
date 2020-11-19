#include "dhcp/timer.h"

#include <signal.h>
#include <cstring>

#include "dhcp/common.h"

namespace dhcp {

namespace details {

void posixTimerStop(sigval value) {
    auto listener = reinterpret_cast<ITimerListener *>(value.sival_ptr);
    if (listener != nullptr)
        listener->onTimer();
}

}  // namespace details

Timer::Timer(ITimerListener * listener) noexcept {
    sigevent event;
    memset(&event, 0, sizeof(event));
    event.sigev_notify = SIGEV_THREAD;
    event.sigev_notify_function = details::posixTimerStop;
    event.sigev_value.sival_ptr = reinterpret_cast<void *>(listener);

    if (timer_create(CLOCK_MONOTONIC, &event, &mTimer) < 0)
        logError("Cannot create timer");
}

Timer::~Timer() noexcept {
    timer_delete(mTimer);
}

void Timer::lease(size_t sec) noexcept {
    itimerspec timespec;
    memset(&timespec, 0, sizeof(timespec));
    timespec.it_value.tv_sec = sec;
    timer_settime(mTimer, 0, &timespec, nullptr);
}

void Timer::release() noexcept {
    lease(0);
}

time_t Timer::remainingTime() const noexcept {
    itimerspec timespec;
    timer_gettime(mTimer, &timespec);
    return timespec.it_value.tv_sec;
}

bool Timer::isStopped() const noexcept {
    return remainingTime() == 0;
}

}  // namespace dhcp
