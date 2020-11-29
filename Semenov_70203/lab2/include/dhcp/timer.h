#pragma once

#include <time.h>
#include <functional>
#include <memory>
#include <mutex>

namespace dhcp {

class ITimerAccessor {
 public:
    virtual ~ITimerAccessor() = default;

    virtual void onTimer() noexcept = 0;
};

class Notifier {
 public:
    Notifier() noexcept;

    void setTimer(ITimerAccessor * timer) noexcept;

    void notify() noexcept;

 private:
    mutable std::mutex mMutex;
    ITimerAccessor * mTimer;
};

class Timer : public ITimerAccessor {
 public:
    using CallbackType = std::function<void()>;

 public:
    Timer() noexcept;

    explicit Timer(CallbackType callback) noexcept;

    Timer(const Timer &) = delete;

    Timer(Timer &&) noexcept;

    ~Timer() noexcept;

    void setCallback(CallbackType callback) noexcept;

    void start(size_t sec) noexcept;

    time_t stop() noexcept;

    time_t remainingTime() const noexcept;

    Timer & operator=(const Timer &) = delete;

    Timer & operator=(Timer &&) noexcept;

 private:
    void onTimer() noexcept override;

    void createTimer() noexcept;

    void timerDelete() noexcept;

    void assign(Timer && other) noexcept;

 private:
    mutable std::mutex mMutex;

    std::unique_ptr<Notifier> mNotifier;
    std::function<void()> mCallback;
    timer_t mPosixTimer;
};

}  // namespace dhcp
