package v.akfz.glazelib.util.gl;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

public class GPUTimer {
	private final String name;
	private final int queryStart;
	private final int queryEnd;
	private long lastTimeNs = 0;
	private boolean pending = false;

	public GPUTimer(String name) {
		this.name = name;
		this.queryStart = GL15.glGenQueries();
		this.queryEnd = GL15.glGenQueries();
	}

	public void begin() {
		GL33.glQueryCounter(queryStart, GL33.GL_TIMESTAMP);
		pending = true;
	}

	public void end() {
		GL33.glQueryCounter(queryEnd, GL33.GL_TIMESTAMP);
	}

	public double pollMs() {
		if (!pending) return lastTimeNs / 1_000_000.0;
		int[] available = new int[1];
		GL15.glGetQueryObjectiv(queryEnd, GL15.GL_QUERY_RESULT_AVAILABLE, available);
		if (available[0] == 0) return -1;
		long[] start = new long[1], end = new long[1];
		GL33.glGetQueryObjecti64v(queryStart, GL15.GL_QUERY_RESULT, start);
		GL33.glGetQueryObjecti64v(queryEnd, GL15.GL_QUERY_RESULT, end);
		lastTimeNs = end[0] - start[0];
		pending = false;
		return lastTimeNs / 1_000_000.0;
	}

	public String getName() { return name; }

	public void cleanup() {
		GL15.glDeleteQueries(queryStart);
		GL15.glDeleteQueries(queryEnd);
	}
}