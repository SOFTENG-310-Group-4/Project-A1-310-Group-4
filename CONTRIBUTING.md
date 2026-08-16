# Contributing to Grocerfy

## How to file a bug report

Before you submit an issue make sure to search through existing issues to see if your question was already answered. 

If your issue has not already mentioned and appears to be a bug then you can create a new issue following the [bug report template](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/blob/main/ISSUE_TEMPLATE.md)

When filing an issue try your best to answer the following: 

•   Overview of the issue
•   What did you do?
•   What did you expect to happen?
•   What happened instead?
•   Operating system and processor architecture (if relevant)
•   Set of steps to replicate the same issue?  

## How to suggest a new feature
• Before creating an issue, search for issues if that feature has already been requested
• Detail why you think that issue would be useful 
• Use the [feature request template](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/blob/main/.github/ISSUE_TEMPLATE/feature_request.md)
• New features must be approved during team meetings

## How to submit a pull request (and what happens next like code review process)
•   See Github for existing pull requests to prevent duplicates
•   Create a new branch to make your changes (e.g. git checkout -b feat/a1-cart-comparison main)
•   Save your changes locally and include test cases
•   Run tests with (./mvnw test)
•   Commit changes with a descriptive message (e.g. git commit - "feat(...): implement...)
•   Push branch to github (e.g. git push origin feat/a1-cart-comparison)

•   Open a pull request
•   Provide a clear title describing what changed
•   At least one member must review and approve the PR before merging (no self-merging)
•   If changes are suggested then make the required changes, recheck if tests are passing and then commit and push to the github repository which will update your pull request. 
•   After your pull request is merged, you can delete your branch and pull the changes from the main repository.

## How to set up your environment and run tests

- Required
•   JDK 17 or higher
•   Node.js (v18+) & npm
•   Git

## How to setup
•   clone directory with
git clone https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4.git
cd Project-A1-310-Group-4

•   create your branch with
```bash
git checkout -b ...
```

Run Java 17 Backend:

### macOS / Linux:
```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

### Windows:
```bash
cd backend
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

### Navigate to the frontend directory and start the React dev server

```bash
cd ../frontend
npm install
npm start
```

### Run tests 
### Backend JUnit 5 / Mockito tests
```bash
cd backend

# On macOS / Linux:
./mvnw test

# On Windows:
mvnw.cmd test
```
### Navigate to the frontend directory and run React tests
```bash
cd frontend
npm test
```

## The types of contributions the project is looking for (and any types you do not want)
• Bug fixes
• Feature implementations
• Documentation improvements
• Performance improvements


## How contributors can get started if they are newcomers (hint: use an issue label to identify issues that are most suitable for newcomers)
• Look on Github repository issues tab
• Look for issues labeled "Good First Issue"
• Leave a comment asking if they can be assigned to it before starting work

## Custom labels for the project
• We have a few custom labels which contributors should use if it is applicable for an issue
• The refactor label should be applied whenever an issue relates to refactoring code
• The test coverage label should be applied whenever an issue relates to tests or test coverage

## Technical requirements for contributions (for example, should tests be included with each code change? links to style guidelines or other project conventions)
•   Code should follow [Google Java Style](https://google.github.io/styleguide/javaguide.html)
•   All logic changes should include unit tests

## Your roadmap or vision for the project
This project aims to inform shoppers about which grocery store is the cheapest and closest
according to their shopping needs. It would allow students to minimise time wasted on grocery shopping by eliminating the need to search across multiple grocery store websites and provide them with the closest stores. It would indirectly encourage them to cook healthier meals by reducing the hassle and cost of grocery shopping.

### Feature Roadmap
A1 (Present)
•	(A1) Search and compare prices of groceries from different retailers
•	(A1) Ability to add items to a virtual shopping cart and calculate the total price
•	(A1) Function to be able to save shopping cart selection across different sessions
•	(A1) Basket price comparison across different supermarkets
•	(A1) Filter items based on selected dietary preferences
A2 (Future)
•	(A2) Item stock status in individual locations 
•	(A2) Show the distance between the user's current location and the store location
•	(A2) Tool to sort and exclude stores based on distance
•	(A2) AI integration that uses the selected items to suggest easy recipes for students
•	(A2) Club card toggle search integration (e.g. Club+, Everyday Rewards Card)

## High level design / architecture design information
React.js Frontend 
- User Interface (HTML, CSS, Javascript)
- Will display products, price comparisons, cart/basket price states, dietary etc

Java 17 Backend 
- Handles calculations, logic

Relational Database
- Stores all mock product catalogs, store details 

## Project ground rules like expected behaviour (link to code of conduct)
•   (code of conduct link placeholder)

## How contributors should (or should not) get in touch with you
•   Contributors should get in touch by for non-urgent issues by messaging the discord or bringing it up in Discord meetings. When using discord, make sure to use the correct channels. 
•   Emails are accepted for getting in touch provided it is semi-urgent 
