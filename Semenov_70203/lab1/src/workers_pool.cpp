#include "workers_pool.h"

#include <stdexcept>

WorkersPool::WorkersPool(int size) {
    if (size <= 0)
        throw std::invalid_argument{"Cannot create zero workers"};

    for (auto i = 0; i < size; i++) {
        auto worker = std::make_unique<Worker>(*this);
        mWorkers.emplace_back(std::move(worker));
    }
}

WorkersPool::~WorkersPool() noexcept {
    MANUAL_FINISH
}

void WorkersPool::join() noexcept {
    for (auto & worker : mWorkers)
        worker->join();
}

void WorkersPool::addTask(Task && task) noexcept {
    {
        std::lock_guard lock{mMutex};
        mTasks.emplace_back(std::move(task));
    }
    mConditional.notify_one();
}

size_t WorkersPool::size() const noexcept {
    return mWorkers.size();
}

WorkersPool::Task WorkersPool::waitForTask() noexcept {
    std::unique_lock lock{mMutex};
    mConditional.wait(
        lock, [this]() { return !mTasks.empty() || isStopped(); });

    if (!mTasks.empty()) {
        auto task = std::move(mTasks.front());
        mTasks.pop_front();
        return task;
    }

    // Stopped
    for (auto & worker : mWorkers)
        worker->stop();

    return nullptr;
}

void WorkersPool::onStart() noexcept {
    for (auto & worker : mWorkers)
        worker->start();
}

void WorkersPool::onStop() noexcept {
    mConditional.notify_all();
}
