# DSA Hub frontend

React and Vite frontend foundation for DSA Hub. React Router defines the placeholder application routes; API calls use relative `/api/...` URLs and Vite proxies them to the local Spring Boot backend at `http://localhost:8080` during development.

## Development

From this directory:

```sh
npm install
npm run dev
```

Build the production frontend with:

```sh
npm run build
```

The frontend is organized into API modules, authentication and progress contexts, routes, and guest progress localStorage helpers. Page components and visual design will be added later.
