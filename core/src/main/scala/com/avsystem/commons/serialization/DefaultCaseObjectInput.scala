package com.avsystem.commons
package serialization

//package com.avsystem.commons
//package serialization
//
//import com.avsystem.commons.serialization.GenCodec.ReadFailure
//
//final class DefaultCaseObjectInput(firstField: FieldInput, actualInput: ObjectInput, caseFieldName: String)
//  extends ObjectInput {
//
//  private var atFirstField = true
//
//  override def knownSize: Int = actualInput.knownSize
//
//  def hasNext: Boolean = atFirstField || actualInput.hasNext
//
//  def nextField(): FieldInput =
//    if (atFirstField) {
//      atFirstField = false
//      firstField
//    } else {
//      val field = actualInput.nextField()
//      if (field.fieldName == caseFieldName) {
//        throw new ReadFailure(s"$caseFieldName field found too far into the object")
//      }
//      field
//    }
//}
