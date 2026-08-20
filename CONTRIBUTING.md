# Contributing to Grocerfy

## Code of Conduct (For ground rules and expected behaviour)
Before contributing to this project, make sure to read our [Code of Conduct](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/blob/main/CODE_OF_CONDUCT.md) to ensure a productive, friendly and collaborative
environment. 

## How to file a bug report

Before you submit an issue make sure to search through existing issues to see if your question was already answered. 

If your issue has not already been mentioned and appears to be a bug then you can create a new issue following the [bug report template](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/blob/main/.github/ISSUE_TEMPLATE/bug_report.md)

When filing an issue try your best to answer the following: 

- Overview of the issue
- What did you do?
- What did you expect to happen?
- What happened instead?
- Operating system and processor architecture (if relevant)
- Set of steps to replicate the same issue?  

## How to suggest a new feature
- Before creating an issue, search for issues if that feature has already been requested

- Detail why you think that issue would be useful 

- Use the [feature request template](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/blob/main/.github/ISSUE_TEMPLATE/feature_request.md)

- New features must be approved during team meetings

## How to submit a pull request (and what happens next)
- See Github for existing pull requests to prevent duplicates

- Make sure you are working out of your own forked repository of the main group repository

- Make sure your fork is synced with the main repository when making changes e.g.

```bash
# First add your main repository if not done already e.g.
git remote add upstream https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4

git checkout main
git pull upstream main
git push origin main
```
- Have your work on a new branch when making changes

### Our naming convention for branches 

Start with the category prefix and then title, which is separated by hyphens.

- **feature/**: For new features or functionalities.

- **bugfix/**: For fixing bugs in the code.

- **hotfix/**: For urgent patches, usually applied to 
production.

- **design/**: For user interface or user experience updates.

- **refactor/**: For improving code structure without changing functionality.

- **test/**: For writing or improving automated tests.

- **doc/**: For documentation updates.

#### Then you can...

1.  Save your changes locally and run against test cases

2.  Commit changes with a descriptive message

3.  Push branch to your personal fork (origin)

4.  On your personal fork repository you can click the yellow "Compare & pull request" banner that appears at the top.

5.  Remember to set Base repository, Base, Head repository and Compare to the right locations. 

6.  Add a clear title, and include what you worked on in the description and reference the issue number e.g. closes #17.

7.  At least one member must review and approve the PR before merging (no self-merging)

8.  If changes are suggested then make the required changes, recheck if tests are passing and then commit and push to the github repository which will update your pull request. 

9.  After your pull request is merged, you can delete your feature branch (locally and on your fork) and pull the latest changes from upstream.


## How to set up your environment and run tests

### Run Java 17 Backend:

#### Required:

- JDK 17 or higher
- Node.js (v18+) & npm
- Git

Clone your personal fork directory with:
```bash
git clone https://github.com/<YOUR_GITHUB_USERNAME>/Project-A1-310-Group-4
```

Once in project terminal
```bash
cd server

# macOS / Linux:
./mvnw clean install
./mvnw spring-boot:run

# Windows (in Command Prompt drop .\ prefix):
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

### Navigate to the frontend directory and start the React dev server
Must have [Node.js](https://nodejs.org/en/download) installed.

```bash
cd client
npm install
npm run dev
```
You should now see the frontend at http://localhost:5173/

### Tests 
#### Backend JUnit 5 / Mockito tests
```bash
cd server

# macOS / Linux:
./mvnw clean compile test

# Windows (in Command Prompt drop .\ prefix):
.\mvnw.cmd clean compile test
```
#### Frontend directory React tests
```bash
cd client
npm test
```

Further in-depth instructions of the above can be found inside our [README.md](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/blob/main/README.md#installation)

## Types of contributions 

#### Examples of contributions Grocerfy is looking for
- Bug fixes
- Feature implementations
- Documentation improvements
- Performance improvements

#### Examples of contributions we do not want
- Uncoordinated/Abrupt code dumps
- Duplicate work
- Changes made out of scope of the project or issue it is addressing
- Work that does not address an active issue 


## How contributors can get started if they are newcomers 
- Look on Github repository issues tab

- Look for issues labeled "Good First Issue"

- Leave a comment asking if they can be assigned to it before starting work

## Custom labels
We have a few custom labels which contributors should use if it is applicable for an issue.

Label | Meaning
---- | ----
`A1` | Planned for the Assignment 1 release
`A2` | Deferred to the Assignment 2 iteration
`frontend` | UI and user experience work
`backend` | Java application logic and data model
`security` | Raised by Snyk or otherwise security-related
`refactor` | Improving code structure without changing functionality
`test coverage` | Adding or improving automated tests

These custom labels can also be found in the [wiki](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/wiki/Project-Management#labels).

## Technical requirements for contributions
- Code should follow [Google Java Style](https://google.github.io/styleguide/javaguide.html)

-  All logic changes should include unit tests

## Our roadmap/vision for Grocerfy
This project aims to inform shoppers about which grocery store is the cheapest and closest
according to their shopping needs. It would allow students to minimise time wasted on grocery shopping by eliminating the need to search across multiple grocery store websites and provide them with the closest stores. It would indirectly encourage them to cook healthier meals by reducing the hassle and cost of grocery shopping.

### Feature Roadmap
#### A1 (Present)

- (A1) Search and compare prices of groceries from different retailers

- (A1) Ability to add items to a virtual shopping cart and calculate the total price

- (A1) Function to be able to save shopping cart selection across different sessions

- (A1) Basket price comparison across different supermarkets

- (A1) Filter items based on selected dietary preferences

#### A2 (Future)
- (A2) Item stock status in individual locations

- (A2) Show the distance between the user's current location and the store location

- (A2) Tool to sort and exclude stores based on distance

- (A2) AI integration that uses the selected items to suggest easy recipes for students

- (A2) Club card toggle search integration (e.g. Club+, Everyday Rewards Card)

## High level design / architecture design information
#### React.js Frontend 
- User Interface (HTML, CSS, Javascript)
- Will display products, price comparisons, cart/basket price states, dietary etc

#### Java 17 Backend 
- Handles calculations, logic

#### Relational Database
- Stores all mock product catalogs, store details 

## [License](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/blob/main/LICENSE)
By contributing to Grocerfy you acknowledge that your contributions will be licensed under the MIT license in this project.

## How contributors should get in touch with us
Contributors have two primary ways of getting in touch:

- [Discord](https://discord.gg/DGVxjj47Dc)

- [GitHub Discussions](https://github.com/SOFTENG-310-Group-4/Project-A1-310-Group-4/discussions)