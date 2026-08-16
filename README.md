# Grocerfy 🛒
Grocerfy is a web application designed to help students and shoppers on a budget browse grocery prices across mainstream supermarket chains in New Zealand (e.g., Woolworths, PAK’nSAVE).

## What Does This Project Do?
Grocerfy lets users build a virtual shopping cart and filter options based on dietary preferences (e.g., gluten-free, vegetarian), and as a result, determine which supermarket offers the lowest total cost for their basket. It would also show the user which grocery store is the closest and/or the most affordable in their set radius.

## Why Is This Project Useful?
Grocerfy is designed to address the challenges of making grocery shopping more affordable and convenient. It aims to:
* **Save Money:** Compare prices across nearby supermarkets to find the cheapest option.
* **Save Time:** Comparison calculations are done automatically for the user.
* **Support Dietary Needs:** Users can search for products based on specific health and dietary requirements.
* **Encourage Home Cooking:** Help students maintain healthier diets by making home cooking more accessible.

## Getting Started
### Pre-requisites 
* To run the backend server, you will need [Java 17+](https://www.oracle.com/anz/java/technologies/downloads/). We recommend Java 17, but higher versions should be fine
* To run the web server, you will need [Node.js](https://nodejs.org/en/download) installed (includes npm with installation). We recommend version v.24.19.0

**Note:** Maven does not need to be installed separately — this project uses the Maven Wrapper (`mvnw`/`mvnw.cmd`), which downloads the correct Maven version automatically. If you already have Maven installed, you can substitute `mvn` for `./mvnw` (Mac/Linux) or `.\mvnw.cmd` (Windows).
### Installation 
**Initial Step:** Clone the repository into your desired path with `git clone https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4.git`.
#### Frontend dependencies
After installing Node.js, open a **new** terminal window (or restart your IDE) before running `npm` commands.
1. Navigate to the client folder with `cd client`
2. Run `npm install` to install the frontend dependencies. 
    - On Windows, if `npm` commands are blocked with a "running scripts is disabled" error, run `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser` once in PowerShell. 
### Deployment
**Note:** For the best experience, run the backend and frontend servers in separate terminals. The backend server **must** be started before launching the frontend server.
#### Backend
1. From the project root, navigate to the server folder with `cd server`
2. Start the backend server with `./mvnw spring-boot:run` (Mac/Linux) or `.\mvnw.cmd spring-boot:run` (Windows).
3. Near the bottom of the terminal, you should see "Started GrocerfyApplication". This server runs on http://localhost:8080
#### Frontend
1. From the project root, navigate to the client folder with `cd client`
2. Start the web server with `run npm dev`
3. The terminal should show a link to http://localhost:5173
### Testing Installation
#### Running Test Suites
* Backend:
    * Navigate to the server folder with `cd server`
    * Then run `./mvnw clean compile test` (Mac/Linux) or `.\mvnw.cmd clean compile test` (Windows)
* Frontend
    * Navigate to the client folder wtih `cd client`
    * Then run `npm test`

**Note:** Running the frontend test suites does not require the backend server to be running.
#### Verifying Visually
* Backend:
    * After launching server, the terminal shows "Started GrocerfyApplication" without errors. Then visit http://localhost:8080/api/product, you should see a JSON response with the different products
    * If you want to view the seeded data directly, then visit http://localhost:8080/h2-console instead. Use JDBC URL `jdbc:h2:mem:grocerfy`, username `sa`, and a blank password
* Frontend:
    * When lauching the server (after backend), confirm the terminal shows a `Local: http://localhost:5173/` URL. Visit the local address in a browser - you should see the Grocerfy app. No errors should be in the browser console (F12 -> Console)


## License Details
This project is licensed under the **MIT License**. More details can be found in the [LICENSE](LICENSE) file.

## Versions
* v1.0.0 (A1 Release)

## Getting Help
If you've found a bug in this repository or would like to request the addition of a new feature, then please refer to our [Contributing Guidelines](CONTRIBUTING.md) for more details on how to get started.

For more context on the project - including meeting minutes, who worked on what, and even mistakes made during development - please check out our [Wiki](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/wiki).

## Contributor Guidelines
Any contributions to Grocerfy are more than welcome! If you are interested, please visit our [Contributing Guidelines](CONTRIBUTING.md) for more details on how to participate.

## Tech Stack & Acknowledgements
### Technology Stack
* **Backend:** Java 17, Spring Boot 4.1.0, Spring Data JPA, H2 (in-memory database)
* **Frontend:** React 18, Vite
* **Testing:** Vitest (Frontend), JUnit 5 + Spring Boot test stack (Backend)
* **Code Quality:** SonarCloud, Snyk
### Major Contributors
* **Initial project developers (A1):** Bryan Quach, Jeffrey Song, Mansher Bhullar, Takahiro Kanaizumi, Tevita 'Akau'ola, Wayne Tian.