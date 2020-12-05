#pragma once

#include "manual_control.h"
#include "task.h"
#include "worker.h"

#include <condition_variable>
#include <deque>
#include <memory>
#include <mutex>
#include <vector>

class WorkersPoolAccessor {
public:
    using Task = std::unique_ptr<ITask>;
    using TaskList = std::deque<Task>;
    using WorkersList = std::vector<std::unique_ptr<Worker>>;

public:
    virtual ~WorkersPoolAccessor() noexcept = default;

    virtual Task waitForTask() noexcept = 0;
};

class WorkersPool : public WorkersPoolAccessor, public ManualControl {
public:
    explicit WorkersPool(int size);

    ~WorkersPool() noexcept;

    void join() noexcept override;

    void addTask(Task && task) noexcept;

    size_t size() const noexcept;

private:
    void onStart() noexcept override;

    void onStop() noexcept override;

    Task waitForTask() noexcept override;

private:
    TaskList mTasks;
    WorkersList mWorkers;
    std::condition_variable mConditional;
    std::mutex mMutex;
};
