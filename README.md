# Traffic Light Simulator with Java

A multi-threaded traffic light controller written in Java. This project demonstrates the implementation of a **dynamic circular scheduling algorithm** and real-time state management.

## System Highlights
- **Real-Time Concurrency:** Uses a background worker thread to manage the "heartbeat" of the intersection, ensuring that the traffic signal rotation remains accurate despite user-initiated menu interrupts.
- **Dynamic Circular Queue:** Employs an `AtomicReferenceArray` to manage a thread-safe, rotating buffer of roads. The system calculates wait times based on the relative position of the active road within the circular queue.
- **Robust Error Handling:** Engineered to manage edge cases such as mid-cycle road deletion and non-contiguous road states, ensuring the system reaches a consistent state without crashing or data race conditions.

## 🛠 Technical Architecture

A high-concurrency Java application designed to manage traffic flow across multiple roads with dynamic timing, scheduling, and real-time state monitoring. The system follows a producer-consumer architecture where:
1. **The UI Thread** handles user commands (Add/Delete/System status).
2. **The Worker Thread** handles the timing logic and the "Green Light" rotation.

## 🚀 Overview
The **Traffic Management System** serves as a centralized controller for road scheduling. It allows users to:
* **Add Roads:** Register new roads to the system, automatically linking them into the circular timing queue.
* **Delete Roads:** Remove roads from the queue, with the system intelligently recalculating intervals for all remaining roads to maintain timing integrity.
* **Monitor Flow:** Observe the system state in real-time, visualizing which roads are "open" or "closed" and the remaining time for each.

---

The core of the system relies on a **Circular Buffer** pattern, managed via `AtomicReferenceArray` and various `AtomicInteger` pointers. By centralizing state mutations within a synchronized runnable loop, the system avoids race conditions and ensures data consistency even under high-frequency updates.



### Key Concurrency Features
* **Thread Safety:** Implemented using `synchronized` methods and atomic variables to ensure that the UI thread (`Main`) and the background worker (`QueueThread`) never work on inconsistent data snapshots.
* **Dynamic Re-indexing:** When a road is added or deleted, the system recalculates the timing intervals for all affected roads, ensuring that the total cycle time remains coherent.
* **Graceful Lifecycle Management:** Uses `volatile` flags and `Thread.join()` to ensure clean system shutdowns, preventing resource leaks or orphaned threads.



---

## 🏃 How to Run

### Prerequisites
* **Java Development Kit (JDK):** Ensure you have JDK 11 or higher installed.

### Steps
1. **Clone the repository and run the following:**
   ```bash
   git clone [https://github.com/Vinesse-Nevertheless/Traffic-Light-Simulator-with-Java.git]
   cd traffic-management-system
   javac -d . src/traffic/Main.java
   java traffic.Main

   
2. Follow the on-screen prompts:
3. Input the number of roads and the desired time interval in seconds.
4. Use the numeric menu options to add/delete roads or view the system state.

## Key Learnings
- Managing race conditions in shared-memory environments.
- Designing data structures that remain consistent even when elements are removed or added dynamically.
- Mastering the interaction between blocking console I/O and non-blocking background tasks.
