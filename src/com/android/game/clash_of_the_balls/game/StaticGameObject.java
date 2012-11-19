package com.android.game.clash_of_the_balls.game;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.android.game.clash_of_the_balls.Texture;
import com.android.game.clash_of_the_balls.VertexBufferFloat;

/**
 * game object that does not move, but can collide with moving objects
 * 
 * texture is optional
 *
 */
public class StaticGameObject extends GameObject {
	
	public enum Type {
		Background,
		Hole,
		Obstacle,
		Player,
		Item
	}
	
	public final Type m_type;
	
	public final int m_id; //object id: this is unique across a game
						   //used by the network to identify a dynamic game object
						   //(it is not used for background objects)
						   //the lowest id is 1
	
	protected Texture m_texture;
	protected VertexBufferFloat m_color_data;
	protected VertexBufferFloat m_position_data;
	
	StaticGameObject(final int id, Vector position, Type type, Texture texture) {
		super(position);
		m_type = type;
		m_id = id;
		m_texture = texture;
		
		m_position_data = new VertexBufferFloat(VertexBufferFloat.sprite_position_data, 3);
		m_color_data = new VertexBufferFloat(VertexBufferFloat.sprite_color_data_white, 4);
	}

	@Override
	public void draw(RenderHelper renderer) {
		
		drawTexture(renderer);
		
		doModelTransformation(renderer);
		
		//position data
		int position_handle = renderer.shaderManager().a_Position_handle;
		if(position_handle != -1)
			m_position_data.apply(position_handle);
		
        // color
		int color_handle = renderer.shaderManager().a_Color_handle;
		if(color_handle != -1)
			m_color_data.apply(color_handle);
		
		renderer.apply();
		
        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);                               
        
        undoModelTransformation(renderer);
	}
	
	protected void drawTexture(RenderHelper renderer) {
		if(m_texture != null) {
			renderer.shaderManager().activateTexture(0);
			m_texture.useTexture(renderer);
		} else {
			renderer.shaderManager().deactivateTexture();
		}
	}
	
	//these can be overridden by subclasses to customize model transformation
	protected void doModelTransformation(RenderHelper renderer) {
		//translate
		int model_mat_pos = renderer.pushModelMat();
		float model_mat[] = renderer.modelMat();
		Matrix.translateM(model_mat, model_mat_pos, 
				m_position.x-0.5f, m_position.y-0.5f, 0.f);
	}
	
	protected void undoModelTransformation(RenderHelper renderer) {
		renderer.popModelMat();
	}
	

	@Override
	public void move(float dsec) {
		// nothing to do
	}
}