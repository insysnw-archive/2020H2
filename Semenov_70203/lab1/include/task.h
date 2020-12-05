#pragma once

class ITask {
public:
    virtual ~ITask() noexcept = default;

    virtual void run() noexcept = 0;
};
