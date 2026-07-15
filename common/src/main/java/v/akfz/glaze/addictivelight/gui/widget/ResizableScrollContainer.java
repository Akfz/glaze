package v.akfz.glaze.addictivelight.gui.widget;

import lombok.Getter;
import lombok.Setter;
import v.akfz.aslib.gui.widget.api.AbstractGroupWidget;
import v.akfz.aslib.render.color.ColorUtils;
import v.akfz.aslib.gui.widget.api.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;

public class ResizableScrollContainer extends AbstractGroupWidget {
    @Setter
    @Getter
    private int contentWidth;
    @Setter
    @Getter
    private int contentHeight;
    @Getter
    private int scrollOffsetX = 0;
    @Getter
    private int scrollOffsetY = 0;

    private boolean draggingScrollbarY = false;
    private boolean draggingScrollbarX = false;

    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean wasMiddlePressed = false;

    @Setter
    @Getter
    protected int scrollbarColor = ColorUtils.rgbToArgb(255, 255, 255, 255);
    @Setter
    @Getter
    protected int scrollbarBgColor = ColorUtils.rgbToArgb(255, 80, 80, 80);
    @Getter
    @Setter
    private int bgColor = 0x19000000;

    public ResizableScrollContainer(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.contentWidth = width;
        this.contentHeight = height;
    }

    public void setWidth(int newWidth) {
        this.width = newWidth;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && getFocused() != null) {
            getFocused().setFocused(false);
        }
    }

    public void setVisible(boolean v){
        this.visible = v;
    }

    @Override
    protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        boolean isMiddlePressed = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

        if (isMiddlePressed) {
            if (!wasMiddlePressed && isMouseOver(mouseX, mouseY) && visible) {
                wasMiddlePressed = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            } else if (wasMiddlePressed) {
                double deltaX = mouseX - lastMouseX;
                double deltaY = mouseY - lastMouseY;

                if (contentWidth > width) {
                    setScrollOffsetX(scrollOffsetX - (int) deltaX);
                }
                if (contentHeight > height) {
                    setScrollOffsetY(scrollOffsetY - (int) deltaY);
                }

                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }
        } else {
            wasMiddlePressed = false;
        }

        graphics.enableScissor(x, y, x + width, y + height);
        graphics.fill(x, y, x + width, y + height, bgColor);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x - scrollOffsetX, y - scrollOffsetY, 0);

        int correctedMouseX = (int) (mouseX - (x - scrollOffsetX));
        int correctedMouseY = (int) (mouseY - (y - scrollOffsetY));

        for (AbstractWidget child : children) {
            if (child.isVisible() &&
                    child.getX() + child.getWidth() > scrollOffsetX && child.getX() < scrollOffsetX + width &&
                    child.getY() + child.getHeight() > scrollOffsetY && child.getY() < scrollOffsetY + height) {
                child.render(graphics, correctedMouseX, correctedMouseY, delta);
            }
        }

        poseStack.popPose();
        graphics.disableScissor();

        if (contentHeight > height) renderScrollbarY(graphics, mouseX, mouseY);
        if (contentWidth > width) renderScrollbarX(graphics, mouseX, mouseY);
    }

    private void renderScrollbarY(GuiGraphics graphics, int mouseX, int mouseY) {
        int barHeight = Math.max(10, (int) ((float) height / contentHeight * height));
        int maxScrollY = contentHeight - height;
        int barY = y + (int) ((float) scrollOffsetY / maxScrollY * (height - barHeight));
        int barX = x + width - 6;

        graphics.fill(barX, y, barX + 4, y + height, scrollbarBgColor);

        boolean hovered = mouseX >= barX && mouseX < barX + 4 && mouseY >= barY && mouseY < barY + barHeight;
        int color = (draggingScrollbarY || hovered) ? ColorUtils.brighten(scrollbarColor, 30) : scrollbarColor;
        graphics.fill(barX, barY, barX + 4, barY + barHeight, color);
    }

    private void renderScrollbarX(GuiGraphics graphics, int mouseX, int mouseY) {
        int barWidth = Math.max(10, (int) ((float) width / contentWidth * width));
        int maxScrollX = contentWidth - width;
        int barX = x + (int) ((float) scrollOffsetX / maxScrollX * (width - barWidth));
        int barY = y + height - 6;

        graphics.fill(x, barY, x + width, barY + 4, scrollbarBgColor);

        boolean hovered = mouseX >= barX && mouseX < barX + barWidth && mouseY >= barY && mouseY < barY + 4;
        int color = (draggingScrollbarX || hovered) ? ColorUtils.brighten(scrollbarColor, 30) : scrollbarColor;
        graphics.fill(barX, barY, barX + barWidth, barY + 4, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;

        if (button == 0) {
            if (contentHeight > height && mouseX >= x + width - 6 && mouseX < x + width - 2) {
                draggingScrollbarY = true;
                return true;
            }

            if (contentWidth > width && mouseY >= y + height - 6 && mouseY < y + height - 2) {
                draggingScrollbarX = true;
                return true;
            }
        }

        double correctedMouseX = mouseX - (x - scrollOffsetX);
        double correctedMouseY = mouseY - (y - scrollOffsetY);

        boolean childClicked = false;
        for (AbstractWidget child : children) {
            if (child.isVisible() && child.isMouseOver(correctedMouseX, correctedMouseY)) {
                if (child.mouseClicked(correctedMouseX, correctedMouseY, button)) {
                    setFocused(child);
                    this.setDragging(true);
                    childClicked = true;
                    break;
                }
            }
        }

        if (!childClicked) {
            setFocused(null);
        }
        return childClicked;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbarY = false;
        draggingScrollbarX = false;
        this.setDragging(false);

        double correctedMouseX = mouseX - (x - scrollOffsetX);
        double correctedMouseY = mouseY - (y - scrollOffsetY);

        if (getFocused() != null) {
            return getFocused().mouseReleased(correctedMouseX, correctedMouseY, button);
        }
        return super.mouseReleased(correctedMouseX, correctedMouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScrollbarY) {
            int barHeight = Math.max(10, (int) ((float) height / contentHeight * height));
            int scrollAreaY = height - barHeight;
            if (scrollAreaY > 0) {
                float scrollBy = (float) deltaY / scrollAreaY * (contentHeight - height);
                setScrollOffsetY(scrollOffsetY + (int) scrollBy);
            }
            return true;
        }

        if (draggingScrollbarX) {
            int barWidth = Math.max(10, (int) ((float) width / contentWidth * width));
            int scrollAreaX = width - barWidth;
            if (scrollAreaX > 0) {
                float scrollBy = (float) deltaX / scrollAreaX * (contentWidth - width);
                setScrollOffsetX(scrollOffsetX + (int) scrollBy);
            }
            return true;
        }

        double correctedMouseX = mouseX - (x - scrollOffsetX);
        double correctedMouseY = mouseY - (y - scrollOffsetY);

        if (getFocused() != null) {
            return getFocused().mouseDragged(correctedMouseX, correctedMouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(correctedMouseX, correctedMouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;

        double correctedMouseX = mouseX - (x - scrollOffsetX);
        double correctedMouseY = mouseY - (y - scrollOffsetY);

        for (AbstractWidget child : children) {
            if (child.isVisible() && child.isMouseOver(correctedMouseX, correctedMouseY)) {
                if (child.mouseScrolled(correctedMouseX, correctedMouseY, amount)) return true;
            }
        }

        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            if (contentWidth > width) {
                setScrollOffsetX(scrollOffsetX - (int) (amount * 14));
                return true;
            }
        } else {
            if (contentHeight > height) {
                setScrollOffsetY(scrollOffsetY - (int) (amount * 14));
                return true;
            }
        }
        return false;
    }

    public void scrollTo(int offsetX, int offsetY) {
        setScrollOffsetX(offsetX);
        setScrollOffsetY(offsetY);
    }

    public void scrollToWidget(AbstractWidget widget) {
        if (children.contains(widget)) {
            scrollTo(widget.getX() - x, widget.getY() - y);
        }
    }

    public void setScrollOffsetX(int scrollOffsetX) { this.scrollOffsetX = Math.max(0, Math.min(contentWidth - width, scrollOffsetX)); }
    public void setScrollOffsetY(int scrollOffsetY) { this.scrollOffsetY = Math.max(0, Math.min(contentHeight - height, scrollOffsetY)); }
}