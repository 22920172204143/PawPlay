package com.pawhunt.app.preview

import kotlin.math.*

/** Navigation and local poses share actual speed and one fixed-step clock. */
class BugMotion(private val config: Config, private var rest: Double = 13.0) {
    data class State(val id: String, val label: String, val speed: Pair<Double, Double>, val duration: Pair<Double, Double>)
    data class Elastic(val pulse: Pair<Double, Double>, val accelerationStretch: Double,
                       val stiffness: Double, val damping: Double)
    data class Config(val seed: Long, val arenaRadius: Double, val maxSpeed: Double,
                      val acceleration: Double, val braking: Double, val responseSeconds: Double,
                      val transitionSeconds: Double, val maxOpening: Double, val states: List<State>,
                      val wingFrequency: Pair<Double, Double>, val bodySway: Pair<Double, Double>,
                      val antennaSway: Pair<Double, Double>, val antennaStiffness: DoubleArray,
                      val antennaDamping: DoubleArray, val wingExcursion: Pair<Double, Double>,
                      val bodyElastic: Elastic, val antennaRootFrequency: Pair<Double, Double>,
                      val antennaRootSway: Pair<Double, Double>)
    class Pose(rest: Double, frequency: Double) {
        var x = 0.0; var z = 0.0; var height = .018
        var yaw = 0.0; var pitch = 0.0; var roll = 0.0
        var speed = 0.0; var energy = 0.0
        var scaleX = 1.0; var scaleY = 1.0; var scaleZ = 1.0
        var wingLeft = rest; var wingRight = rest; var wingHz = frequency
        var swayAmplitude = 0.0
        val antennae = DoubleArray(6)
        var label = "缓行"; var activity = "cruise"
    }
    companion object {
        private const val STEP = 1.0 / 120
        private fun clamp(x: Double, a: Double = 0.0, b: Double = 1.0) = x.coerceIn(a, b)
        private fun mix(a: Double, b: Double, t: Double) = a + (b - a) * t
        private fun smooth(value: Double): Double { val x=clamp(value); return x*x*(3-2*x) }
        private fun wrap(x: Double) = atan2(sin(x), cos(x))
        private fun decay(dt: Double, tau: Double) = 1-exp(-dt/tau)
        private fun stroke(phase: Double, open: Double, hold: Double): Double {
            val p = phase-floor(phase); val end = open+hold
            return when {
                p < open -> smooth(p/open)*2-1
                p < end -> 1.0
                else -> 1-smooth((p-end)/(1-end))*2
            }
        }
    }
    private var randomState = config.seed
    private var selected = "auto"; private var active = 0
    private var time = 0.0; private var age = 0.0; private var accumulator = 0.0
    private var phase = 0.0; private var activation = 0.0
    private var heading = 0.0; private var turnRate = 0.0; private var acceleration = 0.0
    private var wide = 0.0; private var wideTarget = 0.0
    private var goalX = 1.7; private var goalZ = -2.2
    private var duration = 0.0; private var targetSpeed = 0.0
    private val antennaVelocity = DoubleArray(6)
    private var antennaRootPhase = 0.0
    private var bodyStretch = 0.0; private var bodyStretchVelocity = 0.0
    private val targets = DoubleArray(6)
    val pose = Pose(rest, config.wingFrequency.first)
    init { choose(0) }
    private fun random(): Double {
        randomState=randomState*48271%2147483647
        return randomState.toDouble()/2147483647
    }
    private fun range(pair: Pair<Double, Double>) = mix(pair.first, pair.second, random())
    private fun choose(index: Int) {
        active=index; age=0.0
        val state=config.states[index]
        duration=range(state.duration); targetSpeed=range(state.speed)
        wideTarget=if (state.id=="dash" && random()<.42) 1.0 else 0.0
    }
    fun select(id: String) {
        val index=config.states.indexOfFirst { it.id==id }
        require(id=="auto" || index>=0) { "Unknown locomotion: $id" }
        selected=id; choose(if (id=="auto") 0 else index)
    }
    fun setRestOpening(degrees: Double) {
        rest=clamp(degrees,0.0,config.maxOpening); activation=0.0
        pose.wingLeft=rest; pose.wingRight=rest
    }
    private fun chooseGoal() {
        val angle=random()*PI*2; val radius=.9+random()*1.7
        goalX=cos(angle)*radius; goalZ=sin(angle)*radius
        if (hypot(goalX-pose.x,goalZ-pose.z)<1.1) { goalX=-goalX; goalZ=-goalZ }
    }
    @JvmOverloads
    fun advance(seconds: Double, onStep: ((Pose) -> Unit)? = null): Pose {
        require(seconds.isFinite() && seconds>=0) { "Invalid delta" }
        accumulator+=min(seconds,1.0)
        while (accumulator+1e-10>=STEP) { tick(); onStep?.invoke(pose); accumulator-=STEP }
        return pose
    }
    private fun tick() {
        val c=config; val p=pose
        if (age>=duration) {
            var next=active
            if (selected=="auto") next=if (c.states[active].id=="cruise") { if(random()<.78) 1 else 2 } else 0
            choose(next)
        }
        age+=STEP; time+=STEP; activation+=STEP
        if(hypot(goalX-p.x,goalZ-p.z)<.60) chooseGoal()
        val stoppingDistance=p.speed*.25+p.speed*p.speed/(2*c.braking)
        val predictedX=p.x-sin(heading)*stoppingDistance
        val predictedZ=p.z-cos(heading)*stoppingDistance
        val avoidEdge=hypot(predictedX,predictedZ)>c.arenaRadius-.45
        val targetX=if(avoidEdge) 0.0 else goalX; val targetZ=if(avoidEdge) 0.0 else goalZ
        val wantedHeading=atan2(-(targetX-p.x),-(targetZ-p.z))
        val headingError=wrap(wantedHeading-heading)
        val wantedTurn=clamp(headingError*3.0,-2.8,2.8)
        turnRate=mix(turnRate,wantedTurn,decay(STEP,.18))
        var speedGoal=targetSpeed*mix(1.0,.38,clamp((abs(headingError)-.45)/1.5))
        if(avoidEdge) speedGoal=min(speedGoal,.40)
        val wantedAcceleration=clamp((speedGoal-p.speed)*3.8,-c.braking,c.acceleration)
        acceleration=mix(acceleration,wantedAcceleration,decay(STEP,c.responseSeconds))
        p.speed=clamp(p.speed+acceleration*STEP,0.0,c.maxSpeed)
        heading=wrap(heading+turnRate*STEP)
        p.x-=sin(heading)*p.speed*STEP; p.z-=cos(heading)*p.speed*STEP
        val energy=clamp(p.speed/c.maxSpeed)
        p.energy=energy; p.activity=c.states[active].id
        p.label=if(acceleration>.35) "加速" else if(acceleration<-.35) "减速" else c.states[active].label
        wide=mix(wide,wideTarget,decay(STEP,.38))
        val drive=smooth((energy-.12)/.68)
        p.wingHz=mix(c.wingFrequency.first+energy*3,c.wingFrequency.second,drive)
        phase+=p.wingHz*STEP
        val folded=12-drive*3
        val excursion=mix(1.1+energy*20,mix(c.wingExcursion.first,c.wingExcursion.second,wide),drive)
        val amplitude=excursion/2; val center=folded+amplitude
        val open=mix(.65,.30,energy); val hold=mix(.11,.04,energy)
        val fade=smooth(activation/c.transitionSeconds)
        p.wingLeft=mix(rest,center+amplitude*stroke(phase,open,hold),fade)
        p.wingRight=mix(rest,center+amplitude*.95*stroke(phase+.035*sin(time*.87),open,hold),fade)
        val bodyPhase=phase*PI
        p.swayAmplitude=mix(c.bodySway.first,c.bodySway.second,energy)
        p.yaw=heading+sin(bodyPhase)*p.swayAmplitude*PI/180
        p.roll=(sin(bodyPhase+.6)*p.swayAmplitude*.40-turnRate*energy*.85)*PI/180
        p.pitch=(sin(bodyPhase*2+.2)*energy*1.5-acceleration*.45)*PI/180
        p.height=.018+.032*energy+.005*energy*sin(bodyPhase*2)
        // Local volume-preserving squash/stretch with acceleration recoil.
        val elastic=c.bodyElastic
        val stretchTarget=clamp(acceleration/(if(acceleration>=0) c.acceleration else c.braking),-1.0,1.0)*elastic.accelerationStretch
        bodyStretchVelocity+=(elastic.stiffness*(stretchTarget-bodyStretch)-elastic.damping*bodyStretchVelocity)*STEP
        bodyStretch+=bodyStretchVelocity*STEP
        val pulse=mix(elastic.pulse.first,elastic.pulse.second,drive)*sin(bodyPhase-.65)
        p.scaleX=1+pulse-bodyStretch*.55
        p.scaleZ=1+bodyStretch+pulse*.35
        p.scaleY=1/(p.scaleX*p.scaleZ)
        val antenna=mix(c.antennaSway.first,c.antennaSway.second,energy)
        // Slower basal bending stays visible when the wings reach 10 Hz.
        antennaRootPhase+=mix(c.antennaRootFrequency.first,c.antennaRootFrequency.second,energy)*PI*2*STEP
        val rootSway=mix(c.antennaRootSway.first,c.antennaRootSway.second,energy)
        val turnLag=-turnRate*2.0*(.08+energy); val force=acceleration
        targets[0]=sin(antennaRootPhase+.3)*rootSway+turnLag*.65-force*.85
        targets[1]=sin(bodyPhase-.4)*antenna*.95+turnLag*.75-force*2.2
        targets[2]=sin(bodyPhase-.9)*antenna*1.30+turnLag-force*3.4
        targets[3]=sin(antennaRootPhase+1.15)*rootSway*.88+turnLag*.60+force*.80
        targets[4]=sin(bodyPhase+.45)*antenna*.86+turnLag*.75+force*2.0
        targets[5]=sin(bodyPhase-.1)*antenna*1.18+turnLag+force*3.1
        for(i in 0 until 6) {
            val part=i%3; val k=c.antennaStiffness[part]; val damping=c.antennaDamping[part]
            antennaVelocity[i]+=(k*(targets[i]-p.antennae[i])-damping*antennaVelocity[i])*STEP
            p.antennae[i]+=antennaVelocity[i]*STEP
        }
    }
}
