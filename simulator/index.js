const express = require("express");
const cors = require("cors");
const http = require("http");
const { Worker } = require("worker_threads");

const app = express();
app.use(express.json());
app.use(cors({origin: "*"}));
const server = http.createServer(app);

const eventWorkers = {}

const runWorker = (eventID, idx) => {
    let { seats: { min, max }, delay, abandon } = eventWorkers[eventID]
    eventWorkers[eventID].workers[idx]?.postMessage({
        idx,
        delay,
        seats: Math.floor(Math.random() * (1 + max - min)) + min,
        abandon,
        eventID
    });
}

const startWorkers = async (eventID) => {
    let { numWorkers, workers } = eventWorkers[eventID];
    for(let i = 0; i < numWorkers; i++) {
        workers.push(new Worker("./worker/index.js"));
        workers[i]?.on("message", listenToWorker);
        runWorker(eventID, i);
        await new Promise(r => setTimeout(r, 300))
    }
}

const stopWorkers = async (eventID) => {
    let { workers } = eventWorkers[eventID]
    while(workers.length > 0) {
        workers[0]?.terminate();
        workers.shift();
    }
    eventWorkers[eventID].status = "idle"
}

const listenToWorker = (data) => {
    const { status, idx, eventID } = data;
    console.log(status);
    if(status === "Seats unavailable") return stopWorkers(eventID);
    return runWorker(eventID, idx);
}

app.get("/simulate/event-status/:eventID", (req, res) => {
    const { eventID } = req.params;
    if(eventWorkers[eventID]) {
        return res.send({status: eventWorkers[eventID].status})
    }
    res.send({status: "idle"})
});

app.post("/simulate/start-workers", async (req, res) => {
    const {
        eventID,
        numWorkers,
        seats,
        delay,
        abandon
    } = req.body;

    if(eventWorkers[eventID]?.status !== "running") {
        eventWorkers[eventID] = {
            status: "running",
            numWorkers,
            seats,
            delay,
            abandon,
            workers: []
        }
        startWorkers(eventID);
        res.send({status: "running"});
    }
    else res.send({error: "Event simulation already running"});
});

app.get("/simulate/stop-workers/:eventID", (req, res) => {
    const { eventID } = req.params;
    if(eventWorkers[eventID]) {
        stopWorkers(eventID)
    }
    res.send({status: "idle"})
});

server.listen(8081, () => console.log("Listening on port 8081"))