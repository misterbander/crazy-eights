package misterbander.sandboxtabletop.model

interface ServerObject
{
	val id: Int
	var x: Float
	var y: Float
	var rotation: Float
	
	fun setPosition(x: Float, y: Float)
}
