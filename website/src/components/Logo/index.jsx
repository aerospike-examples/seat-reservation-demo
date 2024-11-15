import styles from "./index.module.css";
import { useModal } from "../../hooks/useModal";
import Modal from "../Modal";
import { useCallback, useEffect, useState } from "react";
import clsx from "clsx";

const Logo = ({eventID, setSections, setVenueKey}) => {
    const apiUrl = import.meta.env.VITE_APP_API_URL;
    const simUrl = import.meta.env.VITE_APP_SIM_URL;

    const { modalProps, ref, openModal, closeModal } = useModal();

    const [shortcuts, setShortcuts] = useState(false);
    const [running, setRunning] = useState(true);
    const [numWorkers, setNumWorkers] = useState(50);
    const [seats, setSeats] = useState({min: 2, max: 5});
    const [delay, setDelay] = useState(2);
    const [abandon, setAbandon] = useState(15);

    const getEventStatus = async () => {
        fetch(`${simUrl}/simulate/event-status/${eventID}`)
        .then(response => {
            return response.json()
        })
        .then(({status}) => {
            setRunning(status === "running")
        })
        .catch(err => console.log(err));
    }

    const startWorkers = async () => {
        fetch(`${simUrl}/simulate/start-workers/`, {
            headers: {
				"Content-Type": "application/json"
			},
			method: 'POST',
			body: JSON.stringify({eventID, numWorkers, seats, delay, abandon})
        })
        .then(response => {
            return response.json()
        })
        .then(({status}) => {
            setRunning(status === "running");
            closeModal();
        })
        .catch(err => console.log(err));
    }

    const stopWorkers = () => {
        fetch(`${simUrl}/simulate/stop-workers/${eventID}`)
        .then(response => {
            return response.json()
        })
        .then(({status}) => {
            setRunning(status === "running")
        })
        .catch(err => console.log(err));
    }

    const resetEvent = async () => {
        await fetch(`${apiUrl}/rpc/resetConcert`, {
			headers: {
				"Content-Type": "application/json"
			},
			method: 'POST',
			body: JSON.stringify({concertId: eventID})
        })
        let response = await fetch(`${apiUrl}/concerts/${eventID}/seats`);
        let data = await response.json();
        sessionStorage.removeItem(eventID);
        setSections(data);
        setVenueKey(prev => prev + 1);
    }

    const handleKeys = useCallback((e) => {
        switch(e.code) {
            case "KeyS":
                if(!running) return startWorkers();
                break;
            case "KeyT":
                if(!running) return stopWorkers();
                break;
            case "KeyR":
                if(!running) return resetEvent();
                break;
            default:
                return;
        }
    }, [running]);

    const toggleShortcuts = () => {
        setShortcuts(!shortcuts);
        if(!shortcuts) document.addEventListener("keydown", handleKeys);
        else document.removeEventListener("keydown", handleKeys);
    }

    useEffect(() => {
        getEventStatus();
    }, []);

    return (
        <>
        <img src="/logo.png" alt="Ticketfaster Logo" className={styles.logo} onClick={openModal} />
        {modalProps.open &&
            <Modal {...modalProps} ref={ref} title="Simulator">
                <div className={styles.container}>
                    <div className={styles.shortcuts}>
                        <span>Keybord shortcuts</span>
                        <div className={styles.shortcutControls}>
                            <button className={styles.button} onClick={toggleShortcuts}>{shortcuts ? "Disable" : "Enable"}</button>
                            <span><kbd>s</kbd>tart</span>
                            <span>s<kbd>t</kbd>op</span>
                            <span><kbd>r</kbd>eset</span>    
                        </div>                   
                    </div>
                    <div className={styles.options}>
                        <label className={styles.option} title="Total workers to deploy">
                            <span>Workers </span>
                            <div className={styles.input}>
                                <span>{numWorkers}</span>
                                <input type="range" min={1} max={100} value={numWorkers} onChange={(e) => setNumWorkers(e.currentTarget.value)} disabled={running}/>
                            </div>
                        </label>
                        <label className={styles.option} title="Min value in range for random seat selection">
                            <span>Seat Min </span>
                            <div className={styles.input}>
                                <span>{seats.min}</span>
                                <input type="range" min={1} max={seats?.max} value={seats?.min} onChange={(e) => setSeats(prev => ({...prev, min: e.target.value}))} disabled={running}/>
                            </div>
                        </label>
                        <label className={styles.option} title="Max value in range for random seat selection">
                            <span>Seat Max </span>
                            <div className={styles.input}>
                                <span>{seats.max}</span>
                                <input type="range" min={seats?.min} max={20} value={seats?.max} onChange={(e) => setSeats(prev => ({...prev, max: e.target.value}))} disabled={running}/>
                            </div>
                        </label>
                        <label className={styles.option} title="Delay, in seconds, between adding to cart and purchase | abandon">
                            <span>Delay </span>
                            <div className={styles.input}>
                                <span>{delay}s</span>
                                <input type="range" min={1} max={20} value={delay} onChange={(e) => setDelay(e.currentTarget.value)} disabled={running}/>
                            </div>
                        </label>
                        <label className={styles.option} title="Percent of abandoned carts">
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