#include "worker.h"

#include "workers_pool.h"

Worker::Worker(WorkersPoolAccessor & pool) noexcept : mPool{pool} {}

Worker::~Worker() noexcept {
    MANUAL_FINISH
}

void Worker::threadStep() noexcept {
    auto task = mPool.waitForTask();
    if (task != nullptr)
        task->run();
}
