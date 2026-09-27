package com.worship.nityamandir
import com.worship.nityamandir.engine.*
import org.junit.Assert.*
import org.junit.Test
class SingleIdolSessionTest {
    @Test fun singleIdolCompletesWithoutASecondHiddenTarget() {
        var session=WorshipSession(deityCount=1,lit=true).next()
        session=session.copy(bathed=setOf(0)).next()
        session=session.copy(tilak=setOf(0)).next()
        session=session.copy(flowers=setOf(0)).next()
        session=session.copy(bellRung=true).next()
        session=session.copy(conchBlown=true).next()
        session=session.copy(prasadOffered=true).next()
        session=session.copy(aartiComplete=true).next()
        assertTrue(session.complete)
    }
    @Test fun twoIdolsStillRequireBothTargets() {
        val session=WorshipSession(step=WorshipStep.BATH,bathed=setOf(0))
        assertFalse(session.canContinue)
        assertTrue(session.copy(bathed=setOf(0,1)).canContinue)
    }
}
