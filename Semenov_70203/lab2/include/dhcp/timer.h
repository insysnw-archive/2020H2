#pragma once

#include <time.h>
#include <vector>

namespace dhcp {

class ITimerListener {
 public:
    virtual ~ITimerListener() = default;

    virtual void onTimer() noexcept = 0;
};

class Timer {
 public:
    explicit Timer(ITimerListener * listener) noexcept;

    Timer(const Timer &) = delete;

    Timer(Timer &&) noexcept;

    ~Timer() noexcept;

    void lease(size_t sec) noexcept;

    void release() noexcept;

    time_t remainingTime() const noexcept;

    bool isStopped() const noexcept;

    Timer & operator=(const Timer &) = delete;

    Timer & operator=(Timer &&) noexcept;

 private:
    timer_t mTimer;
    ITimerListener * mListener;
};

}  // namespace dhcp
