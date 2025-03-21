Below is a README file tailored for your Kotlin-based code editor project. It includes a project description, key features, requirements, installation steps, and usage instructions to help users set up and run the application.

---

# Code Editor Application

## Project Description

This is a lightweight code editor built with **Kotlin** and **JavaFX**, designed to let users write, execute, and debug Kotlin scripts within a simple graphical interface. The application provides a code area for script input, an output console for viewing execution results, and features like syntax highlighting and error navigation. It’s an ideal tool for developers experimenting with Kotlin scripts or building educational tools.

## Features

- **Syntax Highlighting**: Highlights Kotlin keywords (e.g., `fun`, `val`, `if`) for improved readability.
- **Error Highlighting**: Marks compilation errors in the code with clickable links to jump to the error’s location.
- **Output Console**: Displays script execution output and error messages in a list view below the code area.
- **Script Execution**: Run Kotlin scripts with a single click and see results instantly.
- **Cancel Functionality**: Script execution can be cancelled (implemented in the backend but not yet exposed in the UI).

## Requirements

To run this project, ensure you have the following installed:

- **JDK 11 or higher**: Required for JavaFX and Kotlin compatibility.
- **Kotlin Compiler (`kotlinc`)**: Must be available in your system’s PATH to compile and run scripts.
- **JavaFX SDK**: Provides the UI framework (download from [GluonHQ](https://gluonhq.com/products/javafx/)).
- **RichTextFX Library**: Powers the code area with advanced text editing features (available on [GitHub](https://github.com/FXMisc/RichTextFX)).
- **An IDE (Recommended)**: IntelliJ IDEA with the Kotlin plugin is suggested for the easiest setup.

## Installation

Follow these steps to set up the project on your machine:

1. **Clone the Repository**  
   Clone this project to your local machine using Git:
   ```sh
   git clone https://github.com/cedbanana/KCodeEditor.git
   ```
2. **Navigate to the Project Directory**  
   Move into the project folder:
   ```sh
   cd KCodeEditor
   ```

3. **Set Up Your IDE (Recommended: IntelliJ IDEA)**  
   - Open IntelliJ IDEA and select `File > Open`, then choose the project folder.
   - Ensure the **Kotlin plugin** is installed (go to `File > Settings > Plugins` and search for "Kotlin").
   - Add the **JavaFX SDK**:
     - Go to `File > Project Structure > Libraries`.
     - Click `+`, select `Java`, and navigate to the `lib` folder of your JavaFX SDK (e.g., `path/to/javafx-sdk/lib`).
   - Add the **RichTextFX library**:
     - Download the JAR from [GitHub](https://github.com/FXMisc/RichTextFX/releases) or use a dependency manager (see below).
     - In `Project Structure > Libraries`, click `+`, select `Java`, and add the RichTextFX JAR.

4. **Optional: Dependency Management with Gradle**  
   If you prefer using Gradle, create a `build.gradle` file with these dependencies:
   ```groovy
   dependencies {
       implementation "org.openjfx:javafx-controls:17"
       implementation "org.fxmisc.richtext:richtextfx:0.10.5"
   }
   ```
   Adjust versions as needed based on the latest releases.

5. **Verify `kotlinc` in PATH**  
   Open a terminal and run:
   ```sh
   kotlinc -version
   ```
   If it’s not recognized, add the Kotlin compiler to your system’s PATH (consult Kotlin’s [installation guide](https://kotlinlang.org/docs/command-line.html)).

## Running the Application

You can run the project either through an IDE or the command line.

### Option 1: Using IntelliJ IDEA
1. Open the project in IntelliJ IDEA.
2. Ensure JavaFX and RichTextFX are configured in the project structure (see Installation step 3).
3. Locate the `CodeEditorApp` class in `src/com/app`.
4. Click the green "Run" button next to the `main` function, or right-click and select `Run 'CodeEditorApp'`.

### Option 2: Using the Command Line
1. Compile the Kotlin files:
   ```sh
   kotlinc -cp "path/to/javafx-sdk/lib/*;path/to/richtextfx.jar" -d out src/com/app/*.kt
   ```
   - Replace `path/to/javafx-sdk/lib/*` with the path to your JavaFX SDK’s `lib` folder.
   - Replace `path/to/richtextfx.jar` with the path to the RichTextFX JAR.
   - On Unix-like systems (Linux/macOS), use `:` instead of `;` as the classpath separator.

2. Run the application:
   ```sh
   java -cp "out;path/to/javafx-sdk/lib/*;path/to/richtextfx.jar" com.app.CodeEditorApp
   ```
   Again, adjust the separator (`;` or `:`) based on your OS.

## Usage

Once the application is running, here’s how to use it:

1. **Write a Script**  
   Type your Kotlin script in the code area (e.g., `println("Hello, World!")`).

2. **Run the Script**  
   Click the **"Submit"** button to execute the script. The output (or errors) will appear in the list view below.

3. **Navigate Errors**  
   If errors occur, they’ll be highlighted in the code area. Click an error message in the output console while holding `Ctrl` (Windows/Linux) or `Cmd` (macOS) to jump to the error’s location.

4. **Cancel Execution**  
   The backend supports cancelling scripts (via `ScriptRunner.cancel()`), but this isn’t yet available in the UI. To add this, extend the `CodeEditorController` to include a "Cancel" button.

## Additional Notes

- **Kotlin Compiler**: The app relies on `kotlinc` to execute scripts, so it must be accessible in your PATH.
- **Syntax Highlighting**: Currently highlights keywords only. Modify the `Highlighter` class to add support for strings, comments, etc.
- **Extensibility**: The modular design (e.g., `ScriptRunner`, `Highlighter`) makes adding new features like additional languages or UI enhancements easy.
- **Windows Support**: Unfortunately, the project is not yet ready to run on Windows Machines.

