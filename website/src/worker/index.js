const sleep = (seconds) =>  new Promise(r => setTimeout(r, seconds * 1000))

const addToCart = async (cartID, numSeats, eventID, available, attempt = 0) => {
    let seats = [];
    let idx = Math.floor(Math.random * (0 + (available.length - numSeats) - 1));        
    for(let i = 0; i < numSeats; i++) seats.push(available[idx + i]);
    let response = await fetch(`https://ticket-website.aerospike.com/concerts/${eventID}/shopping-carts`, {
        headers: {
            "Content-Type": "application/json"
        },
        method: 'POST',
        body: JSON.stringify({id: cartID, seats})
    })
    if(response.ok) return;
    if(attempt < 5) {
        attempt++;
        return await addToCart(cartID, numSeats, eventID, available, attempt)
    }
    throw new Error("fail");
}

const purchaseSeats = async (cartID, eventID) => {
    let response = await fetch(`https://ticket-website.aerospike.com/concerts/${eventID}/purchases`, {
        headers: {
            "Content-Type": "application/json"
        },
        method: 'POST',
        body: JSON.stringify({id: cartID})
    })
    if(response.ok) return
    throw new Error("fail");
}

const abandonSeats = async (cartID, eventID) => {
    let response = await fetch(`https://ticket-website.aerospike.com/concerts/${eventID}/shopping-carts/${cartID}`, {
        method: 'DELETE'
    })
    if(response.ok) return
    throw new Error("fail");
}

const simulateUser = async (delay, seats, abandon, eventID, available) => {
    return new Promise(async (resolve, reject) => {
        let cartID = Math.floor(Math.random() * (Math.random() * 100000000000)).toString();
        addToCart(cartID, seats, eventID, available)
        .then( async () => {
            await sleep(delay);
            let random = Math.random();
            if(random < (abandon / 100)){
                await abandonSeats(cartID, eventID);
            }
            else {
                await purchaseSeats(cartID, eventID);
            }
            await sleep(delay);
            resolve();
        }, (err) => reject(err));
    });
}

self.onmessage = async (e) => {
    const { idx, delay, seats, abandon, eventID, available} = e.data;
    simulateUser(delay, seats, abandon, eventID, available)
    .then(
        () => self.postMessage({status: "ok", idx}),
        () => self.postMessage({status: "fail", idx})
    );
};