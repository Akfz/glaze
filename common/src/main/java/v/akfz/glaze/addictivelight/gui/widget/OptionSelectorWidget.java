package v.akfz.glaze.addictivelight.gui.widget;

import v.akfz.aslib.render.gui.widget.api.render.RenderPart;
import v.akfz.aslib.render.gui.widget.impl.button.ButtonWidget;

import java.util.function.Consumer;

public class OptionSelectorWidget extends ButtonWidget {
    private final String label;
    private final int[] options;
    private int currentIndex;
    private final Consumer<Integer> onSelect;

    public OptionSelectorWidget(int x, int y, int width, int height, String label, int[] options, int initialValue, RenderPart renderer, Consumer<Integer> onSelect) {
        super(x, y, width, height, "");
        this.label = label;
        this.options = options;
        this.onSelect = onSelect;
        this.mainRenderer = renderer;

        this.currentIndex = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == initialValue) {
                this.currentIndex = i;
                break;
            }
        }
        updateText();

        this.setClickFunc((btn, m) -> {
            this.currentIndex = (this.currentIndex + 1) % this.options.length;
            updateText();
            if (this.onSelect != null) {
                this.onSelect.accept(this.options[this.currentIndex]);
            }
        });
    }

    private void updateText() {
        this.text = this.label + ": " + this.options[this.currentIndex];
    }
}