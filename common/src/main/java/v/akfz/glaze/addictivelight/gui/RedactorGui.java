package v.akfz.glaze.addictivelight.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import v.akfz.aslib.gui.widget.api.render.RenderPart;
import v.akfz.aslib.gui.widget.impl.button.ButtonWidget;
import v.akfz.aslib.gui.widget.impl.button.CheckboxWidget;
import v.akfz.aslib.gui.widget.impl.picker.ColorPickerWidget;
import v.akfz.glaze.addictivelight.data.light.LightSource;
import v.akfz.glaze.addictivelight.data.light.LightType;
import v.akfz.glaze.addictivelight.gui.widget.BlenderSpinnerWidget;
import v.akfz.glaze.addictivelight.gui.widget.OptionSelectorWidget;
import v.akfz.glaze.addictivelight.gui.widget.ResizableScrollContainer;
import v.akfz.glaze.addictivelight.render.AddictiveLight;

public class RedactorGui extends Screen {
    private ResizableScrollContainer panel;
    private static boolean alignRight = true;
    private boolean isDeleted = false;
    private boolean showAdvanced = false;

    private float splitRatio = 0.40f;
    private boolean draggingSplit = false;

    private RenderPart blueButton;
    private RenderPart blueCheckbox;

    public RedactorGui(Component title) {
        super(title);
        initRenderers();
    }

    private void initRenderers() {
        this.blueButton = (graphics, mouseX, mouseY, delta, x, y, width, height, extras) -> {
            String text = extras.getOrDefault("text", String.class, "");
            boolean enabled = extras.getOrDefault("enabled", Boolean.class, true);
            boolean pressed = extras.getOrDefault("pressed", Boolean.class, false);
            boolean hovered = extras.getOrDefault("hovered", Boolean.class, false);

            int bg = pressed ? 0xDD003366 : (hovered ? 0x99002244 : 0x66051122);
            int border = hovered ? 0xFF00A2FF : 0xAA0055AA;
            int textCol = enabled ? 0xFFFFFFFF : 0x88888888;

            graphics.fill(x, y, x + width, y + height, bg);
            graphics.renderOutline(x, y, width, height, border);
            graphics.drawString(font, text, x + (width - font.width(text)) / 2, y + (height - font.lineHeight) / 2, textCol, true);
        };

        this.blueCheckbox = (graphics, mouseX, mouseY, delta, x, y, width, height, extras) -> {
            String text = extras.getOrDefault("text", String.class, "");
            boolean checked = extras.getOrDefault("checked", Boolean.class, false);
            boolean hovered = extras.getOrDefault("hovered", Boolean.class, false);

            int boxSize = Math.min(12, height);
            int boxX = x;
            int boxY = y + (height - boxSize) / 2;

            int bg = 0x66051122;
            int border = hovered ? 0xFF00A2FF : 0xAA0055AA;

            graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, bg);
            graphics.renderOutline(boxX, boxY, boxSize, boxSize, border);

            if (checked) {
                graphics.fill(boxX + 2, boxY + 2, boxX + boxSize - 2, boxY + boxSize - 2, 0xFF00A2FF);
            }
            graphics.drawString(font, text, boxX + boxSize + 4, y + (height - font.lineHeight) / 2, 0xFFFFFFFF, true);
        };
    }

    private int getSplitX() {
        int w = this.width;
        int panelW = (int) (w * splitRatio);
        return alignRight ? (w - panelW - 10) : (10 + panelW);
    }

    @Override
    protected void init() {
        int w = this.width;
        int h = this.height;

        int panelW = (int) (w * splitRatio);
        int panelX = alignRight ? (w - panelW - 10) : 10;
        int py = 30;
        int pHeight = h - 45;

        int bgAlpha = 45;
        int bgColor = (bgAlpha << 24) | 0x000A1A;

        this.panel = new ResizableScrollContainer(panelX, py, panelW, pHeight);
        this.panel.setContentWidth(panelW);
        this.panel.setBgColor(bgColor);
        this.panel.setScrollbarColor(0xAA00A2FF);
        this.panel.setScrollbarBgColor(0x33001122);
        this.addRenderableWidget(panel);

        rebuildPanel();

        LightSource<?> l = AddictiveLight.INSTANCE.getRedactor().getPickedSource();
        if (l != null) {
            int pickerW = 180;
            int pickerH = 120;
            int pickerX = alignRight ? 20 : (w - pickerW - 20);
            int pickerY = 40;

            ColorPickerWidget colorPicker = new ColorPickerWidget(pickerX, pickerY, pickerW, pickerH, l.getColor(), v -> {
                LightSource<?> currentLight = AddictiveLight.INSTANCE.getRedactor().getPickedSource();
                if (currentLight != null) {
                    currentLight.color(v);
                    AddictiveLight.INSTANCE.getRedactor().markModified(currentLight);
                }
            });
            colorPicker.setBgColor(0x66051122);
            colorPicker.setBorderColor(0xAA0055AA);
            colorPicker.setSliderBgColor(0x33001122);
            this.addRenderableWidget(colorPicker);
        }
    }

    private void rebuildPanel() {
        panel.clearWidgets();
        int curY = 5;
        int width = panel.getWidth() - 15;

        panel.addWidget(new ButtonWidget(5, curY, width, 18, "Сменить сторону") {{
            this.mainRenderer = blueButton;
            this.setClickFunc((b, m) -> {
                alignRight = !alignRight;
                RedactorGui.this.clearWidgets();
                RedactorGui.this.init();
            });
        }});
        curY += 22;

        panel.addWidget(new ButtonWidget(5, curY, width, 18, "Удалить источник") {{
            this.mainRenderer = blueButton;
            this.setClickFunc((b, m) -> {
                isDeleted = true;
                RedactorGui.this.onClose();
            });
        }});
        curY += 22;

        LightSource<?> l = AddictiveLight.INSTANCE.getRedactor().getPickedSource();
        if (l != null) {
            panel.addWidget(new CheckboxWidget(5, curY, width, 16, "Активен") {{
                this.mainRenderer = blueCheckbox;
                this.setChecked(l.isActive());
                this.setOnToggle(v -> {
                    l.active(v);
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                });
            }});
            curY += 20;

            panel.addWidget(new OptionSelectorWidget(5, curY, width, 18, "Тип источника", new int[]{0, 1, 2, 3, 4}, l.getType().ordinal(), blueButton, v -> {
                l.type(LightType.values()[v]);
                AddictiveLight.INSTANCE.getRedactor().markModified(l);
                rebuildPanel();
            }) {
                @Override
                public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                    this.text = "Тип: " + l.getType().name();
                    super.render(graphics, mouseX, mouseY, delta);
                }
            });
            curY += 22;

            panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, Double.MAX_VALUE, l.getIntensity(), "Интенсивность", false, v -> {
                l.intensity(v.floatValue());
                AddictiveLight.INSTANCE.getRedactor().markModified(l);
            }));
            curY += 22;

            panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, Double.MAX_VALUE, l.getRadius(), "Радиус", false, v -> {
                l.radius(v.floatValue());
                AddictiveLight.INSTANCE.getRedactor().markModified(l);
            }));
            curY += 22;

            if (l.getType() == LightType.AREA_RECTANGLE) {
                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, Double.MAX_VALUE, l.getWidth(), "Ширина", false, v -> {
                    l.width(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, Double.MAX_VALUE, l.getHeight(), "Высота", false, v -> {
                    l.height(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;
            } else if (l.getType() == LightType.SPOT) {
                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 180.0, l.getCutoff(), "Внутренний угол (cutoff)", false, v -> {
                    l.cutoff(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 180.0, l.getOuterCutoff(), "Внешний угол (outer)", false, v -> {
                    l.outerCutoff(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;
            } else if (l.getType() == LightType.AREA_SPHERE) {
                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.01, Double.MAX_VALUE, l.getSourceSize(), "Размер источника", false, v -> {
                    l.sourceSize(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;
            }

            panel.addWidget(new CheckboxWidget(5, curY, width, 16, "Показать доп. настройки") {{
                this.mainRenderer = blueCheckbox;
                this.setChecked(showAdvanced);
                this.setOnToggle(v -> {
                    showAdvanced = v;
                    RedactorGui.this.rebuildPanel();
                });
            }});
            curY += 20;

            if (showAdvanced) {
                panel.addWidget(new CheckboxWidget(5, curY, width, 16, "Динамический") {{
                    this.mainRenderer = blueCheckbox;
                    this.setChecked(l.isDynamic());
                    this.setOnToggle(v -> {
                        l.dynamic(v);
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    });
                }});
                curY += 20;

                panel.addWidget(new CheckboxWidget(5, curY, width, 16, "Сохранять (save)") {{
                    this.mainRenderer = blueCheckbox;
                    this.setChecked(l.isSave());
                    this.setOnToggle(v -> {
                        l.save(v);
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    });
                }});
                curY += 20;

                if (l.getType() != LightType.AREA_RECTANGLE) {
                    panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, Double.MAX_VALUE, l.getWidth(), "Ширина", false, v -> {
                        l.width(v.floatValue());
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    }));
                    curY += 22;

                    panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, Double.MAX_VALUE, l.getHeight(), "Высота", false, v -> {
                        l.height(v.floatValue());
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    }));
                    curY += 22;
                }

                if (l.getType() != LightType.AREA_SPHERE) {
                    panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.01, Double.MAX_VALUE, l.getSourceSize(), "Размер источника", false, v -> {
                        l.sourceSize(v.floatValue());
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    }));
                    curY += 22;
                }

                if (l.getType() != LightType.SPOT) {
                    panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 180.0, l.getCutoff(), "Внутренний угол (cutoff)", false, v -> {
                        l.cutoff(v.floatValue());
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    }));
                    curY += 22;

                    panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 180.0, l.getOuterCutoff(), "Внешний угол (outer)", false, v -> {
                        l.outerCutoff(v.floatValue());
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    }));
                    curY += 22;
                }

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.001, 5.0, l.getLinear(), "Линейное затухание", false, v -> {
                    l.linear(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.001, 5.0, l.getQuadratic(), "Квадратичное затухание", false, v -> {
                    l.quadratic(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.1, 10.0, l.getFalloffExponent(), "Экспонента затухания", false, v -> {
                    l.falloffExponent(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new CheckboxWidget(5, curY, width, 16, "Тени") {{
                    this.mainRenderer = blueCheckbox;
                    this.setChecked(l.isShadowsEnabled());
                    this.setOnToggle(v -> {
                        l.shadowsEnabled(v);
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    });
                }});
                curY += 20;

                panel.addWidget(new CheckboxWidget(5, curY, width, 16, "Игнорировать блоки") {{
                    this.mainRenderer = blueCheckbox;
                    this.setChecked(l.isIgnoreBlocks());
                    this.setOnToggle(v -> {
                        l.ignoreBlocks(v);
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    });
                }});
                curY += 20;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 5.0, l.getShadowSoftness(), "Мягкость теней", false, v -> {
                    l.shadowSoftness(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 0.1, l.getShadowBias(), "Смещение тени", false, v -> {
                    l.shadowBias(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.01, 10.0, l.getShadowNear(), "Ближняя плоскость тени", false, v -> {
                    l.shadowNear(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 1.0, 512.0, l.getShadowFar(), "Дальняя плоскость тени", false, v -> {
                    l.shadowFar(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new CheckboxWidget(5, curY, width, 16, "Объемный свет (Volumetric)") {{
                    this.mainRenderer = blueCheckbox;
                    this.setChecked(l.isVolumetric());
                    this.setOnToggle(v -> {
                        l.volumetric(v);
                        AddictiveLight.INSTANCE.getRedactor().markModified(l);
                    });
                }});
                curY += 20;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 20.0, l.getVolumetricStrength(), "Сила объема", false, v -> {
                    l.volumetricStrength(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0, 0.99, l.getMieG(), "Анизотропия фазы (Mie G)", false, v -> {
                    l.mieG(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 1.0, l.getFogDensity(), "Плотность тумана", false, v -> {
                    l.fogDensity(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;

                panel.addWidget(new BlenderSpinnerWidget(5, curY, width, 18, 0.0, 1.0, l.getFogAbsorption(), "Поглощение тумана", false, v -> {
                    l.fogAbsorption(v.floatValue());
                    AddictiveLight.INSTANCE.getRedactor().markModified(l);
                }));
                curY += 22;
            }
        }

        panel.setContentHeight(curY + 10);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int splitX = getSplitX();
        if (button == 0 && mouseX >= splitX && mouseX < splitX + 10 && mouseY >= 30 && mouseY < this.height - 15) {
            this.draggingSplit = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingSplit) {
            this.draggingSplit = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSplit) {
            if (alignRight) {
                this.splitRatio = (float) Math.max(0.15, Math.min(0.85, (this.width - mouseX - 10) / this.width));
            } else {
                this.splitRatio = (float) Math.max(0.15, Math.min(0.85, (mouseX - 10) / this.width));
            }
            this.clearWidgets();
            this.init();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x20000000);

        int splitX = getSplitX();
        int handleColor = draggingSplit || (mouseX >= splitX && mouseX < splitX + 10 && mouseY >= 30 && mouseY < this.height - 15) ? 0xFF00A2FF : 0xAA0055AA;
        graphics.fill(splitX + 3, 30, splitX + 7, this.height - 15, handleColor);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFF00A2FF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        super.onClose();
        AddictiveLight.INSTANCE.getRedactor().sendChanges(isDeleted,AddictiveLight.INSTANCE.getRedactor().getPickedSource());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}