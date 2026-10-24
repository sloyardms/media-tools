package sloyardms.mediatools.menu;

import java.util.ArrayList;
import java.util.List;

/** Numbered text menu that dispatches to registered {@link MenuAction}s until the user picks Exit. */
public class Menu {

    private final String title;
    private final List<MenuAction> actions = new ArrayList<>();

    public Menu(String title) {
        this.title = title;
    }

    public Menu register(MenuAction action) {
        actions.add(action);
        return this;
    }

    public List<MenuAction> actions() {
        return List.copyOf(actions);
    }

    public void run(ConsoleIO io) {
        while (true) {
            io.println();
            io.println("=== " + title + " ===");
            for (int i = 0; i < actions.size(); i++) {
                io.printf("%d) %s%n", i + 1, actions.get(i).label());
            }
            io.println("0) Exit");

            String choice;
            try {
                choice = io.ask("Choose an option: ");
            } catch (InputClosedException e) {
                return;
            }
            if (choice.equals("0")) {
                return;
            }
            if (choice.isEmpty()) {
                continue;
            }

            MenuAction action = parseChoice(choice);
            if (action == null) {
                io.println("Unknown option: " + choice);
                continue;
            }

            io.println();
            try {
                action.run(io);
            } catch (InputClosedException e) {
                return;
            } catch (Exception e) {
                io.println("Error: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
            }
        }
    }

    private MenuAction parseChoice(String choice) {
        try {
            int index = Integer.parseInt(choice) - 1;
            return index >= 0 && index < actions.size() ? actions.get(index) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
