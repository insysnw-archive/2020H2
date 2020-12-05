#pragma once

#include <atomic>

// Should be placed in each derived class destructor to finish all threads
// associated with the object before destroying the object
#define MANUAL_FINISH \
    stop();           \
    join();

class ManualControl {
public:
    ManualControl() noexcept;

    virtual ~ManualControl() noexcept = default;

    virtual void join() noexcept;

    void start() noexcept;

    void stop() noexcept;

    bool isWorking() const noexcept;

    bool isStopped() const noexcept;

protected:
    virtual void onStart() noexcept;

    virtual void onStop() noexcept;

private:
    std::atomic_bool mWorking;
};
