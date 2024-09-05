import { createBrowserRouter, redirect, RouterProvider } from 'react-router-dom';
import App from './app/index.jsx'
import Concert, { concertLoader } from './app/Concert/index.jsx';
import Concerts, { concertsLoader } from './app/Concerts/index.jsx';

const Routes = () => {
    const router = createBrowserRouter([
        {
            path: '/', 
            element: <App />,
            errorElement: <div />,
            children: [{
                errorElement: <div />,
                children: [
                    {index: true, element: <div />, loader: () => redirect("/events")},
                    {path: "events", element: <Concerts />, loader: () => concertsLoader()},
                    {path: "events/:artist/:id", element: <Concert />, loader: ({params}) => concertLoader({...params})}
                ]
            }]
        }
    ]);

    return <RouterProvider router={router} />
}

export default Routes;