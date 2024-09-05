import Venue from "../../components/Venue";
import styles from "./index.module.css";
import { Navigate, useLoaderData, useLocation } from "react-router-dom";

export const concertLoader = async (params) => {
    const { artist, id } = params;
    let response = await fetch(`/concerts/${id}/seats`);
    let seats = await response.json();
    return { artist, seats, id }
}

const Concert = () => {
    const { artist, seats, id } = useLoaderData();
    const { state } = useLocation();
    if(!state) return <Navigate to="/events" /> 
    const { description, date, title } = state;

    return (
        <>
        <div className={styles.concertHeader}>
            <h1>{artist}: {title}</h1>
            <div className={styles.concertHeaderDetails}>
                <strong> {description}</strong><br />
                <strong>
                {new Date(date).toLocaleDateString('en-US', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                })}
                </strong>
            </div>
        </div>
        <Venue seats={seats} eventID={id} />
        </>
    )
}

export default Concert;