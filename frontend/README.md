# PetClinic frontend

A React frontend for Petclinic's GraphQL API. Start the backend first, then:

```bash
npm install    # the first time only
npm run dev
```

Open http://localhost:5173. If the backend runs on a port other than 8080, start the frontend with
`PETCLINIC_BACKEND=http://localhost:8090 npm run dev`.

## How it knows what to show

The frontend sends only the operations in `../src/main/resources/operations` and
`../src/main/resources/operations-ui`, one `.graphql` file each. At startup it reads the backend's
schema and validates every operation against it. A page lights up when its operations validate.
`src/features.js` lists which operations each part needs.
