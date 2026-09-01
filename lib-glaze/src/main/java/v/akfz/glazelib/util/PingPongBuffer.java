package v.akfz.glazelib.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

public class PingPongBuffer {
	private RenderTarget front;
	private RenderTarget back;
	private int width, height;
	private boolean hasDepth;

	public PingPongBuffer(boolean hasDepth) {
		this.hasDepth = hasDepth;
	}

	public void ensureSize(int w, int h) {
		if (width == w && height == h && front != null) return;
		destroy();
		this.width = w;
		this.height = h;
		this.front = new TextureTarget(w, h, hasDepth, Minecraft.ON_OSX);
		this.back = new TextureTarget(w, h, hasDepth, Minecraft.ON_OSX);
	}

	public RenderTarget front() { return front; }
	public RenderTarget back() { return back; }

	public void swap() {
		RenderTarget tmp = front;
		front = back;
		back = tmp;
	}

	public RenderTarget result() { return front; }

	public void destroy() {
		if (front != null) { front.destroyBuffers(); front = null; }
		if (back != null) { back.destroyBuffers(); back = null; }
	}
}