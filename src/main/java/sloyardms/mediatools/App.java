package sloyardms.mediatools;

import javax.imageio.ImageIO;

import sloyardms.mediatools.menu.ConsoleIO;
import sloyardms.mediatools.menu.Menu;
import sloyardms.mediatools.thumbnails.RemoveThumbnailDuplicatesAction;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        // Reading many images is faster without ImageIO's on-disk stream cache.
        ImageIO.setUseCache(false);

        Menu menu = new Menu("Media Tools")
                .register(new RemoveThumbnailDuplicatesAction());

        menu.run(ConsoleIO.system());
    }
}
