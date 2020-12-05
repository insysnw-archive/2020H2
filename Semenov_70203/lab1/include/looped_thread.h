#pragma once

#include "manual_control.h"

#include <thread>

class LoopedThread : public ManualControl {
public:
    ~LoopedThread() noexcept;

    void join() noexcept final;

protected:
    // The threadStep() method is called every time within loop() method.
    // There is no need to loop inside the threadStep method.
    virtual void threadStep() noexcept = 0;

    virtual void onThreadStart() noexcept;

    virtual void onThreadFinish() noexcept;

private:
    void onStart() noexcept final;

    // Runs in a new thread
    void loop() noexcept;

private:
    std::thread mThread;
};
