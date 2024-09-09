import Artist from "../../components/Artist";
import styles from "./index.module.css";
import { useLoaderData } from "react-router-dom";

export const concertsLoader = async () => {
  const apiUrl = import.meta.env.VITE_APP_API_URL;
  const response = await fetch(`${apiUrl}/concerts/getAll`);
    const data = await response.json();
    const concerts = {}
    for(let concert of data) {
      if(!concerts[concert.artist]?.length) {
        concerts[concert.artist] = [concert];
      }
      else concerts[concert.artist].push(concert)
    }
    return { concerts };
}

const Concerts = () => {
    const { concerts } = useLoaderData();

    return (
        <>
        <img src="/logo.png" alt="Ticketfaster Logo" className={styles.logo} />
        <h1 style={{marginBottom: "30px"}}>Upcoming Concerts</h1>
        <div className={styles.concertList}>
          {Object.keys(concerts).map(artist => (
            <Artist artist={artist} shows={concerts[artist]} key={artist}/>
          ))}
        </div>
        </>
    )
}

export default Concerts