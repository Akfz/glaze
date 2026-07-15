package v.akfz.glaze.addictivelight.gui.widget;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import v.akfz.aslib.gui.widget.api.AbstractWidget;
import v.akfz.aslib.gui.widget.impl.text.TextField;

import java.util.Locale;
import java.util.function.Consumer;

public class BlenderSpinnerWidget extends AbstractWidget {
    @Getter
    private double value;
    private final double min;
    private final double max;
    private final double step;
    private final String label;
    private final Consumer<Double> onChange;
    private final boolean integerMode;

    private boolean isDragging = false;
    private double lastMouseX;
    private final double dragSensitivity;

    private final TextField inputField;
    private boolean editingText = false;

    public BlenderSpinnerWidget(int x, int y, int width, int height, double min, double max, double initialValue, String label, boolean integerMode, Consumer<Double> onChange) {
        super(x, y, width, height);
        this.min = min;
        this.max = max;
        this.value = initialValue;
        this.step = integerMode ? 1.0 : 0.1;
        this.dragSensitivity = integerMode ? 0.35 : 0.05;
        this.label = label;
        this.onChange = onChange;
        this.integerMode = integerMode;

        this.inputField = new TextField(0, 0, width, height);
        this.inputField.setText(formatValue(value));
        this.inputField.setLineRenderer((graphics, line, xPos, yPos, idx, startIdx, defColor) -> {});
    }

    private String formatValue(double val) {
        if (integerMode) {
            return String.valueOf((int) val);
        }
        return String.format(Locale.US, "%.2f", val);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        inputField.setFocused(focused);
        if (!focused && editingText) {
            applyTextValue();
        }
    }

    @Override
    protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (editingText && !inputField.isFocused()) {
            applyTextValue();
        }

        boolean hovered = isMouseOver(mouseX, mouseY);
        int bgColor = 0x66051122;
        int borderColor = (isDragging || hovered || inputField.isFocused()) ? 0xFF00A2FF : 0xAA0055AA;

        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.renderOutline(x, y, width, height, borderColor);

        var font = Minecraft.getInstance().font;
        int textColor = (hovered || inputField.isFocused()) ? 0xFFFFFFFF : 0xCCFFFFFF;

        graphics.drawString(font, "<", x + 5, y + (height / 2) - 4, textColor, false);
        graphics.drawString(font, ">", x + width - 10, y + (height / 2) - 4, textColor, false);

        if (inputField.isFocused()) {
            String text = inputField.getText();
            int textWidth = font.width(text);
            int startX = x + (width / 2) - (textWidth / 2);
            int startY = y + (height / 2) - 4;

            if (inputField.hasSelection()) {
                int selMin = inputField.getSelectionStart();
                int selMax = inputField.getSelectionEnd();
                int minIdx = Math.min(selMin, selMax);
                int maxIdx = Math.max(selMin, selMax);

                int selXStart = startX + font.width(text.substring(0, minIdx));
                int selXEnd = startX + font.width(text.substring(0, maxIdx));

                graphics.fill(selXStart, startY - 1, selXEnd, startY + font.lineHeight - 1, 0x5500A2FF);
            }

            graphics.drawString(font, text, startX, startY, textColor, false);

            if (System.currentTimeMillis() % 1000 < 500) {
                int cursorIdx = inputField.getCursorPos();
                int cursorOffset = font.width(text.substring(0, Math.min(cursorIdx, text.length())));
                int cursorX = startX + cursorOffset;
                graphics.fill(cursorX, startY - 1, cursorX + 1, startY + font.lineHeight - 1, textColor);
            }
        } else {
            String displayValue = label + ": " + formatValue(value);
            int textWidth = font.width(displayValue);
            graphics.drawString(font, displayValue, x + (width / 2) - (textWidth / 2), y + (height / 2) - 4, textColor, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            if (mouseX < x + 15) {
                applyValue(value - step);
                return true;
            } else if (mouseX > x + width - 15) {
                applyValue(value + step);
                return true;
            } else {
                if (button == 0) {
                    isDragging = true;
                    lastMouseX = mouseX;
                    if (inputField.isFocused()) {
                        inputField.mouseClicked(mouseX - x, mouseY - y, button);
                    }
                }
                return true;
            }
        }
        if (inputField.isFocused()) {
            applyTextValue();
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging && button == 0) {
            isDragging = false;
            double dist = Math.abs(mouseX - lastMouseX);
            if (dist < 3.0) {
                editingText = true;
                inputField.setFocused(true);
                inputField.setText(formatValue(value));
                inputField.setCursorPos(inputField.getText().length());
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            double diff = mouseX - lastMouseX;
            double sensitivity = net.minecraft.client.gui.screens.Screen.hasShiftDown() ? dragSensitivity * 0.1 : dragSensitivity;
            applyValue(value + diff * sensitivity);
            lastMouseX = mouseX;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                applyTextValue();
                return true;
            }
            return inputField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (inputField.isFocused()) {
            if (Character.isDigit(chr) || chr == '.' || chr == '-' || chr == ',') {
                char finalChar = chr == ',' ? '.' : chr;
                return inputField.charTyped(finalChar, modifiers);
            }
            return false;
        }
        return super.charTyped(chr, modifiers);
    }

    private void applyTextValue() {
        try {
            double parsed = Double.parseDouble(inputField.getText());
            applyValue(parsed);
        } catch (NumberFormatException ignored) {}
        editingText = false;
        inputField.setFocused(false);
    }

    private void applyValue(double newValue) {
        if (integerMode) {
            newValue = Math.round(newValue);
        }
        double clamped = Math.max(min, Math.min(max, newValue));
        if (clamped != this.value) {
            this.value = clamped;
            if (!inputField.isFocused()) {
                inputField.setText(formatValue(value));
            }
            if (onChange != null) onChange.accept(this.value);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}