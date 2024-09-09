import styles from "./index.module.css";
import { useModal } from "../../hooks/useModal";
import Modal from "../Modal";
import { useRef, useState } from "react";
import clsx from "clsx";
import SimWorker from "../../worker?worker";

const Logo = ({eventID, setSections, setVenueKey}) => {
    const workers = useRef([]);
    const failedWorkers = useRef(0);
    const eventSource = useRef();
    const availableSeats = useRef([]);
    const { modalProps, ref, openModal, closeModal } = useModal();
    const [running, setRunning] = useState(false);
    const [numWorkers, setNumWorkers] = useState(50);
    const [seats, setSeats] = useState({min: 2, max: 5});
    const [delay, setDelay] = useState(2);
    const [abandon, setAbandon] = useState(15);
    
    const handleSeats = (key, value) => setSeats(prev => ({...prev, [key]: value}));
    
    const getAvailableSeats = async () => {
        availableSeats.current = [];
        const apiUrl = import.meta.env.VITE_APP_API_URL;
        let response = await fetch(`${apiUrl}/concerts/${eventID}/seats`);
        let data = await response.json();
        for(let i = 0; i < data.length; i++) {
            for(let j = 0; j < data[i].length; j++) {
                for(let k = 0; k < data[i][j].length; k++) {
                    if(data[i][j][k] === 0) {
                        availableSeats.current.push(`${i}-${j}-${k}`)
                    }
                }
            }
        }
    }

    const handleSeatUpdate = (e) => {
        const [ seatID, value ] = e.data.split(":");
        const seatIdx = availableSeats.current.indexOf(seatID);
        if(value === "0" && seatIdx === -1) availableSeats.current.push(seatID);
        else availableSeats.current.splice(seatIdx, 1);
    }
    
    const runWorker = (idx, attempt = 0) => {
        let { min, max } = seats;
        workers.current[idx].postMessage({
            idx,
            delay,
            seats: Math.floor(Math.random() * (1 + max - min)) + min,
            abandon,
            eventID,
            available: availableSeats.current,
            attempt
        });
    }

    const listenToWorker = (e) => {
        const { status, idx, attempt } = e.data;
        switch(status) {
            case "ok":
                return runWorker(idx);
            case "retry":
                return runWorker(idx, attempt + 1);
            case "fail":
                failedWorkers.current++;
        }
        if(failedWorkers.current === numWorkers) {
            stopWorkers();
        }
    }

    const startWorkers = async () => {
        setRunning(true);
        await getAvailableSeats();
        eventSource.current = new EventSource("/concerts/updates");
        eventSource.current.addEventListener(eventID, handleSeatUpdate);
        closeModal();
        failedWorkers.current = 0;
        for(let i = 0; i < numWorkers; i++) {
            workers.current.push(new SimWorker());
            workers.current[i].addEventListener("message", listenToWorker);
            runWorker(i);
            await new Promise(r => setTimeout(r, 300))
        }
    }

    const stopWorkers = () => {
        setRunning(false);
        eventSource.current.removeEventListener(eventID, handleSeatUpdate);
        for(let i = 0; i < numWorkers; i++) {
            workers.current[i]?.terminate();
        }
        workers.current = [];
    }

    const resetEvent = async () => {
        await fetch("/rpc/resetConcert", {
			headers: {
				"Content-Type": "application/json"
			},
			method: 'POST',
			body: JSON.stringify({concertId: eventID})
        })
        const apiUrl = import.meta.env.VITE_APP_API_URL;
        let response = await fetch(`${apiUrl}/concerts/${eventID}/seats`);
        let data = await response.json();
        sessionStorage.removeItem(eventID);
        setSections(data);
        setVenueKey(prev => prev + 1);
    }

    return (
        <>
        <img src="/logo.png" alt="Ticketfaster Logo" className={styles.logo} onClick={openModal} />
        {modalProps.open &&
            <Modal {...modalProps} ref={ref} title="Simulator">
                <div className={styles.container}>
                    <div className={styles.options}>
                        <label className={styles.option}>
                            <span>Workers </span>
                            <div className={styles.input}>
                                <span>{numWorkers}</span>
                                <input type="range" min={1} max={100} value={numWorkers} onChange={(e) => setNumWorkers(e.currentTarget.value)} disabled={running}/>
                            </div>
                        </label>
                        <label className={styles.option}>
                            <span>Seat Min </span>
                            <div className={styles.input}>
                                <span>{seats.min}</span>
                                <input type="range" min={1} max={seats.max} value={seats.min} onChange={(e) => handleSeats("min", e.currentTarget.value)} disabled={running}/>
                            </div>
                        </label>
                        <label className={styles.option}>
                            <span>Seat Max </span>
                            <div className={styles.input}>
                                <span>{seats.max}</span>
                                <input type="range" min={seats.min} max={20} value={seats.max} onChange={(e) => handleSeats("max", e.currentTarget.value)} disabled={running}/>
                            </div>
                        </label>
                        <label className={styles.option}>
                            <span>Delay </span>
                            <div className={styles.input}>
                                <span>{delay}s</span>
                                <input type="range" min={1} max={20} value={delay} onChange={(e) => setDelay(e.currentTarget.value)} disabled={running}/>
                            </div>
                        </label>
                        <label className={styles.option}>
                            <span>Abandon </span>
                            <div className={styles.input}>
                                <span>{abandon}%</span>
                                <input type="range" min={1} max={100} value={abandon} onChange={(e) => setAbandon(e.currentTarget.value)} disabled={running}/>
                            </div>
                        </label>
                    </div>
                    <div className={styles.controls}>
                        {running ?
                            <button className={clsx(styles.button, styles.stop)} onClick={stopWorkers} disabled={!eventID}>Stop</button>
                            :
                            <button className={clsx(styles.button, styles.start)} onClick={startWorkers} disabled={!eventID}>Start</button>
                        }
                        <button className={clsx(styles.button, styles.buttonSecondary)} onClick={resetEvent} disabled={!eventID}>Reset Event</button>
                    </div>
                </div>
            </Modal>
        }
        </>
    )
}

export default Logo;