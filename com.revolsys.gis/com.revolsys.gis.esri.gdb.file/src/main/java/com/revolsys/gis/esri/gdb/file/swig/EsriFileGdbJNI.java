/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.40
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.revolsys.gis.esri.gdb.file.swig;

class EsriFileGdbJNI {
  public final static native int CreateGeodatabase(String jarg1, long jarg2, Geodatabase jarg2_);
  public final static native int OpenGeodatabase(String jarg1, long jarg2, Geodatabase jarg2_);
  public final static native int CloseGeodatabase(long jarg1, Geodatabase jarg1_);
  public final static native int DeleteGeodatabase(String jarg1);
  public final static native int Geodatabase_GetDatasetTypes(long jarg1, Geodatabase jarg1_, long jarg2);
  public final static native int Geodatabase_GetDatasetRelationshipTypes(long jarg1, Geodatabase jarg1_, long jarg2);
  public final static native int Geodatabase_GetChildDatasets(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3, long jarg4);
  public final static native int Geodatabase_GetRelatedDatasets(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3, String jarg4, long jarg5);
  public final static native int Geodatabase_GetDatasetDefinition(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3, long jarg4);
  public final static native int Geodatabase_GetChildDatasetDefinitions(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3, long jarg4);
  public final static native int Geodatabase_GetRelatedDatasetDefinitions(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3, String jarg4, long jarg5);
  public final static native int Geodatabase_GetDatasetDocumentation(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3, long jarg4);
  public final static native int Geodatabase_CreateFeatureDataset(long jarg1, Geodatabase jarg1_, String jarg2);
  public final static native int Geodatabase_CreateTable(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3, long jarg4, Table jarg4_);
  public final static native int Geodatabase_OpenTable(long jarg1, Geodatabase jarg1_, String jarg2, long jarg3, Table jarg3_);
  public final static native int Geodatabase_CloseTable(long jarg1, Geodatabase jarg1_, long jarg2, Table jarg2_);
  public final static native int Geodatabase_Rename(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3, String jarg4);
  public final static native int Geodatabase_Move(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3);
  public final static native int Geodatabase_Delete(long jarg1, Geodatabase jarg1_, String jarg2, String jarg3);
  public final static native int Geodatabase_GetDomains(long jarg1, Geodatabase jarg1_, long jarg2);
  public final static native int Geodatabase_CreateDomain(long jarg1, Geodatabase jarg1_, String jarg2);
  public final static native int Geodatabase_AlterDomain(long jarg1, Geodatabase jarg1_, String jarg2);
  public final static native int Geodatabase_DeleteDomain(long jarg1, Geodatabase jarg1_, String jarg2);
  public final static native int Geodatabase_GetDomainDefinition(long jarg1, Geodatabase jarg1_, String jarg2, long jarg3);
  public final static native int Geodatabase_GetQueryName(long jarg1, Geodatabase jarg1_, String jarg2, long jarg3);
  public final static native int Geodatabase_ExecuteSQL(long jarg1, Geodatabase jarg1_, String jarg2, boolean jarg3, long jarg4, EnumRows jarg4_);
  public final static native long new_Geodatabase();
  public final static native void delete_Geodatabase(long jarg1);
  public final static native int createGeodatabase(String jarg1, long jarg2, Geodatabase jarg2_);
  public final static native int openGeodatabase(String jarg1, long jarg2, Geodatabase jarg2_);
  public final static native int closeGeodatabase(long jarg1, Geodatabase jarg1_);
  public final static native int deleteGeodatabase(String jarg1);
  public final static native int Table_GetDefinition(long jarg1, Table jarg1_, long jarg2);
  public final static native int Table_GetDocumentation(long jarg1, Table jarg1_, long jarg2);
  public final static native int Table_SetDocumentation(long jarg1, Table jarg1_, String jarg2);
  public final static native int Table_GetFieldInformation(long jarg1, Table jarg1_, long jarg2, FieldInfo jarg2_);
  public final static native int Table_AddField(long jarg1, Table jarg1_, String jarg2);
  public final static native int Table_AlterField(long jarg1, Table jarg1_, String jarg2);
  public final static native int Table_DeleteField(long jarg1, Table jarg1_, String jarg2);
  public final static native int Table_GetIndexes(long jarg1, Table jarg1_, long jarg2);
  public final static native int Table_AddIndex(long jarg1, Table jarg1_, String jarg2);
  public final static native int Table_DeleteIndex(long jarg1, Table jarg1_, String jarg2);
  public final static native int Table_CreateSubtype(long jarg1, Table jarg1_, String jarg2);
  public final static native int Table_AlterSubtype(long jarg1, Table jarg1_, String jarg2);
  public final static native int Table_DeleteSubtype(long jarg1, Table jarg1_, String jarg2);
  public final static native int Table_EnableSubtypes(long jarg1, Table jarg1_, String jarg2, String jarg3);
  public final static native int Table_GetDefaultSubtypeCode(long jarg1, Table jarg1_, int[] jarg2);
  public final static native int Table_SetDefaultSubtypeCode(long jarg1, Table jarg1_, int jarg2);
  public final static native int Table_DisableSubtypes(long jarg1, Table jarg1_);
  public final static native int Table_Search__SWIG_0(long jarg1, Table jarg1_, String jarg2, String jarg3, long jarg4, Envelope jarg4_, boolean jarg5, long jarg6, EnumRows jarg6_);
  public final static native int Table_Search__SWIG_1(long jarg1, Table jarg1_, String jarg2, String jarg3, boolean jarg4, long jarg5, EnumRows jarg5_);
  public final static native int Table_CreateRowObject(long jarg1, Table jarg1_, long jarg2, Row jarg2_);
  public final static native int Table_Insert(long jarg1, Table jarg1_, long jarg2, Row jarg2_);
  public final static native int Table_Update(long jarg1, Table jarg1_, long jarg2, Row jarg2_);
  public final static native int Table_Delete(long jarg1, Table jarg1_, long jarg2, Row jarg2_);
  public final static native int Table_IsEditable(long jarg1, Table jarg1_, boolean[] jarg2);
  public final static native int Table_GetRowCount(long jarg1, Table jarg1_, int[] jarg2);
  public final static native int Table_GetExtent(long jarg1, Table jarg1_, long jarg2, Envelope jarg2_);
  public final static native int Table_SetWriteLock(long jarg1, Table jarg1_);
  public final static native int Table_FreeWriteLock(long jarg1, Table jarg1_);
  public final static native int Table_LoadOnlyMode(long jarg1, Table jarg1_, boolean jarg2);
  public final static native long new_Table();
  public final static native void delete_Table(long jarg1);
  public final static native int Row_IsNull(long jarg1, Row jarg1_, String jarg2, boolean[] jarg3);
  public final static native int Row_SetNull(long jarg1, Row jarg1_, String jarg2);
  public final static native int Row_GetOID(long jarg1, Row jarg1_, int[] jarg2);
  public final static native int Row_GetGlobalID(long jarg1, Row jarg1_, long jarg2, Guid jarg2_);
  public final static native int Row_GetGeometry(long jarg1, Row jarg1_, long jarg2, ShapeBuffer jarg2_);
  public final static native int Row_SetGeometry(long jarg1, Row jarg1_, long jarg2, ShapeBuffer jarg2_);
  public final static native int Row_GetShort(long jarg1, Row jarg1_, String jarg2, short[] jarg3);
  public final static native int Row_SetShort(long jarg1, Row jarg1_, String jarg2, short jarg3);
  public final static native int Row_GetInteger(long jarg1, Row jarg1_, String jarg2, int[] jarg3);
  public final static native int Row_SetInteger(long jarg1, Row jarg1_, String jarg2, int jarg3);
  public final static native int Row_GetFloat(long jarg1, Row jarg1_, String jarg2, float[] jarg3);
  public final static native int Row_SetFloat(long jarg1, Row jarg1_, String jarg2, float jarg3);
  public final static native int Row_GetDouble(long jarg1, Row jarg1_, String jarg2, double[] jarg3);
  public final static native int Row_SetDouble(long jarg1, Row jarg1_, String jarg2, double jarg3);
  public final static native int Row_GetDate(long jarg1, Row jarg1_, String jarg2, long jarg3);
  public final static native int Row_SetDate(long jarg1, Row jarg1_, String jarg2, long jarg3);
  public final static native int Row_GetString(long jarg1, Row jarg1_, String jarg2, long jarg3);
  public final static native int Row_SetString(long jarg1, Row jarg1_, String jarg2, String jarg3);
  public final static native int Row_GetGUID(long jarg1, Row jarg1_, String jarg2, long jarg3, Guid jarg3_);
  public final static native int Row_SetGUID(long jarg1, Row jarg1_, String jarg2, long jarg3, Guid jarg3_);
  public final static native int Row_GetXML(long jarg1, Row jarg1_, String jarg2, long jarg3);
  public final static native int Row_SetXML(long jarg1, Row jarg1_, String jarg2, String jarg3);
  public final static native int Row_GetRaster(long jarg1, Row jarg1_, String jarg2, long jarg3, Raster jarg3_);
  public final static native int Row_SetRaster(long jarg1, Row jarg1_, String jarg2, long jarg3, Raster jarg3_);
  public final static native int Row_GetBinary(long jarg1, Row jarg1_, String jarg2, long jarg3, ByteArray jarg3_);
  public final static native int Row_SetBinary(long jarg1, Row jarg1_, String jarg2, long jarg3, ByteArray jarg3_);
  public final static native int Row_GetFieldInformation(long jarg1, Row jarg1_, long jarg2, FieldInfo jarg2_);
  public final static native long new_Row();
  public final static native void delete_Row(long jarg1);
  public final static native int EnumRows_Next(long jarg1, EnumRows jarg1_, long jarg2, Row jarg2_);
  public final static native void EnumRows_Close(long jarg1, EnumRows jarg1_);
  public final static native int EnumRows_GetFieldInformation(long jarg1, EnumRows jarg1_, long jarg2, FieldInfo jarg2_);
  public final static native long new_EnumRows();
  public final static native void delete_EnumRows(long jarg1);
  public final static native int FieldInfo_GetFieldCount(long jarg1, FieldInfo jarg1_, int[] jarg2);
  public final static native int FieldInfo_GetFieldName(long jarg1, FieldInfo jarg1_, int jarg2, long jarg3);
  public final static native int FieldInfo_GetFieldType(long jarg1, FieldInfo jarg1_, int jarg2, long jarg3);
  public final static native int FieldInfo_GetFieldLength(long jarg1, FieldInfo jarg1_, int jarg2, int[] jarg3);
  public final static native int FieldInfo_GetFieldIsNullable(long jarg1, FieldInfo jarg1_, int jarg2, boolean[] jarg3);
  public final static native long new_FieldInfo();
  public final static native void delete_FieldInfo(long jarg1);
  public final static native boolean ShapeBuffer_Allocate(long jarg1, ShapeBuffer jarg1_, long jarg2);
  public final static native long new_ShapeBuffer__SWIG_0(long jarg1);
  public final static native long new_ShapeBuffer__SWIG_1();
  public final static native void delete_ShapeBuffer(long jarg1);
  public final static native void ShapeBuffer_shapeBuffer_set(long jarg1, ShapeBuffer jarg1_, long jarg2);
  public final static native long ShapeBuffer_shapeBuffer_get(long jarg1, ShapeBuffer jarg1_);
  public final static native void ShapeBuffer_allocatedLength_set(long jarg1, ShapeBuffer jarg1_, long jarg2);
  public final static native long ShapeBuffer_allocatedLength_get(long jarg1, ShapeBuffer jarg1_);
  public final static native void ShapeBuffer_inUseLength_set(long jarg1, ShapeBuffer jarg1_, long jarg2);
  public final static native long ShapeBuffer_inUseLength_get(long jarg1, ShapeBuffer jarg1_);
  public final static native boolean ShapeBuffer_IsEmpty(long jarg1, ShapeBuffer jarg1_);
  public final static native void ShapeBuffer_SetEmpty(long jarg1, ShapeBuffer jarg1_);
  public final static native int ShapeBuffer_GetShapeType(long jarg1, ShapeBuffer jarg1_, long jarg2);
  public final static native int ShapeBuffer_GetGeometryType__SWIG_0(long jarg1, ShapeBuffer jarg1_, long jarg2);
  public final static native boolean ShapeBuffer_HasZs(int jarg1);
  public final static native boolean ShapeBuffer_HasMs(int jarg1);
  public final static native boolean ShapeBuffer_HasIDs(int jarg1);
  public final static native boolean ShapeBuffer_HasCurves(int jarg1);
  public final static native boolean ShapeBuffer_HasNormals(int jarg1);
  public final static native boolean ShapeBuffer_HasTextures(int jarg1);
  public final static native boolean ShapeBuffer_HasMaterials(int jarg1);
  public final static native int ShapeBuffer_GetGeometryType__SWIG_1(int jarg1);
  public final static native int PointShapeBuffer_GetPoint(long jarg1, PointShapeBuffer jarg1_, long jarg2, Point jarg2_);
  public final static native int PointShapeBuffer_GetZ(long jarg1, PointShapeBuffer jarg1_, long jarg2);
  public final static native int PointShapeBuffer_GetM(long jarg1, PointShapeBuffer jarg1_, long jarg2);
  public final static native int PointShapeBuffer_GetID(long jarg1, PointShapeBuffer jarg1_, long jarg2);
  public final static native int PointShapeBuffer_Setup(long jarg1, PointShapeBuffer jarg1_, int jarg2);
  public final static native long new_PointShapeBuffer();
  public final static native void delete_PointShapeBuffer(long jarg1);
  public final static native int MultiPointShapeBuffer_GetExtent(long jarg1, MultiPointShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPointShapeBuffer_GetNumPoints(long jarg1, MultiPointShapeBuffer jarg1_, int[] jarg2);
  public final static native int MultiPointShapeBuffer_GetPoints(long jarg1, MultiPointShapeBuffer jarg1_, long jarg2, Point jarg2_);
  public final static native int MultiPointShapeBuffer_GetZExtent(long jarg1, MultiPointShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPointShapeBuffer_GetZs(long jarg1, MultiPointShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPointShapeBuffer_GetMExtent(long jarg1, MultiPointShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPointShapeBuffer_GetMs(long jarg1, MultiPointShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPointShapeBuffer_GetIDs(long jarg1, MultiPointShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPointShapeBuffer_Setup(long jarg1, MultiPointShapeBuffer jarg1_, int jarg2, int jarg3);
  public final static native int MultiPointShapeBuffer_CalculateExtent(long jarg1, MultiPointShapeBuffer jarg1_);
  public final static native long new_MultiPointShapeBuffer();
  public final static native void delete_MultiPointShapeBuffer(long jarg1);
  public final static native int MultiPartShapeBuffer_GetExtent(long jarg1, MultiPartShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPartShapeBuffer_GetNumParts(long jarg1, MultiPartShapeBuffer jarg1_, int[] jarg2);
  public final static native int MultiPartShapeBuffer_GetNumPoints(long jarg1, MultiPartShapeBuffer jarg1_, int[] jarg2);
  public final static native int MultiPartShapeBuffer_GetParts(long jarg1, MultiPartShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPartShapeBuffer_GetPoints(long jarg1, MultiPartShapeBuffer jarg1_, long jarg2, Point jarg2_);
  public final static native int MultiPartShapeBuffer_GetZExtent(long jarg1, MultiPartShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPartShapeBuffer_GetZs(long jarg1, MultiPartShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPartShapeBuffer_GetMExtent(long jarg1, MultiPartShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPartShapeBuffer_GetMs(long jarg1, MultiPartShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPartShapeBuffer_GetNumCurves(long jarg1, MultiPartShapeBuffer jarg1_, int[] jarg2);
  public final static native int MultiPartShapeBuffer_GetCurves(long jarg1, MultiPartShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPartShapeBuffer_GetIDs(long jarg1, MultiPartShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPartShapeBuffer_Setup__SWIG_0(long jarg1, MultiPartShapeBuffer jarg1_, int jarg2, int jarg3, int jarg4, int jarg5);
  public final static native int MultiPartShapeBuffer_Setup__SWIG_1(long jarg1, MultiPartShapeBuffer jarg1_, int jarg2, int jarg3, int jarg4);
  public final static native int MultiPartShapeBuffer_CalculateExtent(long jarg1, MultiPartShapeBuffer jarg1_);
  public final static native int MultiPartShapeBuffer_PackCurves(long jarg1, MultiPartShapeBuffer jarg1_);
  public final static native long new_MultiPartShapeBuffer();
  public final static native void delete_MultiPartShapeBuffer(long jarg1);
  public final static native int MultiPatchShapeBuffer_GetExtent(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPatchShapeBuffer_GetNumParts(long jarg1, MultiPatchShapeBuffer jarg1_, int[] jarg2);
  public final static native int MultiPatchShapeBuffer_GetNumPoints(long jarg1, MultiPatchShapeBuffer jarg1_, int[] jarg2);
  public final static native int MultiPatchShapeBuffer_GetParts(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPatchShapeBuffer_GetPartDescriptors(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPatchShapeBuffer_GetPoints(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2, Point jarg2_);
  public final static native int MultiPatchShapeBuffer_GetZExtent(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPatchShapeBuffer_GetZs(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPatchShapeBuffer_GetMExtent(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPatchShapeBuffer_GetMs(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPatchShapeBuffer_GetIDs(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPatchShapeBuffer_GetNormals(long jarg1, MultiPatchShapeBuffer jarg1_, long jarg2);
  public final static native int MultiPatchShapeBuffer_GetTextures(long jarg1, MultiPatchShapeBuffer jarg1_, int[] jarg2, int[] jarg3, long jarg4, long jarg5);
  public final static native int MultiPatchShapeBuffer_GetMaterials(long jarg1, MultiPatchShapeBuffer jarg1_, int[] jarg2, int[] jarg3, long jarg4, long jarg5);
  public final static native int MultiPatchShapeBuffer_Setup__SWIG_0(long jarg1, MultiPatchShapeBuffer jarg1_, int jarg2, int jarg3, int jarg4, int jarg5, int jarg6, int jarg7);
  public final static native int MultiPatchShapeBuffer_Setup__SWIG_1(long jarg1, MultiPatchShapeBuffer jarg1_, int jarg2, int jarg3, int jarg4, int jarg5, int jarg6);
  public final static native int MultiPatchShapeBuffer_Setup__SWIG_2(long jarg1, MultiPatchShapeBuffer jarg1_, int jarg2, int jarg3, int jarg4, int jarg5);
  public final static native int MultiPatchShapeBuffer_Setup__SWIG_3(long jarg1, MultiPatchShapeBuffer jarg1_, int jarg2, int jarg3, int jarg4);
  public final static native int MultiPatchShapeBuffer_CalculateExtent(long jarg1, MultiPatchShapeBuffer jarg1_);
  public final static native long new_MultiPatchShapeBuffer();
  public final static native void delete_MultiPatchShapeBuffer(long jarg1);
  public final static native boolean ByteArray_Allocate(long jarg1, ByteArray jarg1_, long jarg2);
  public final static native long new_ByteArray__SWIG_0(long jarg1);
  public final static native long new_ByteArray__SWIG_1();
  public final static native void delete_ByteArray(long jarg1);
  public final static native void ByteArray_byteArray_set(long jarg1, ByteArray jarg1_, long jarg2);
  public final static native long ByteArray_byteArray_get(long jarg1, ByteArray jarg1_);
  public final static native void ByteArray_allocatedLength_set(long jarg1, ByteArray jarg1_, long jarg2);
  public final static native long ByteArray_allocatedLength_get(long jarg1, ByteArray jarg1_);
  public final static native void ByteArray_inUseLength_set(long jarg1, ByteArray jarg1_, long jarg2);
  public final static native long ByteArray_inUseLength_get(long jarg1, ByteArray jarg1_);
  public final static native boolean Envelope_IsEmpty(long jarg1, Envelope jarg1_);
  public final static native void Envelope_SetEmpty(long jarg1, Envelope jarg1_);
  public final static native long new_Envelope__SWIG_0();
  public final static native long new_Envelope__SWIG_1(double jarg1, double jarg2, double jarg3, double jarg4);
  public final static native void delete_Envelope(long jarg1);
  public final static native void Envelope_xMin_set(long jarg1, Envelope jarg1_, double jarg2);
  public final static native double Envelope_xMin_get(long jarg1, Envelope jarg1_);
  public final static native void Envelope_yMin_set(long jarg1, Envelope jarg1_, double jarg2);
  public final static native double Envelope_yMin_get(long jarg1, Envelope jarg1_);
  public final static native void Envelope_xMax_set(long jarg1, Envelope jarg1_, double jarg2);
  public final static native double Envelope_xMax_get(long jarg1, Envelope jarg1_);
  public final static native void Envelope_yMax_set(long jarg1, Envelope jarg1_, double jarg2);
  public final static native double Envelope_yMax_get(long jarg1, Envelope jarg1_);
  public final static native void Envelope_zMin_set(long jarg1, Envelope jarg1_, double jarg2);
  public final static native double Envelope_zMin_get(long jarg1, Envelope jarg1_);
  public final static native void Envelope_zMax_set(long jarg1, Envelope jarg1_, double jarg2);
  public final static native double Envelope_zMax_get(long jarg1, Envelope jarg1_);
  public final static native void Point_x_set(long jarg1, Point jarg1_, double jarg2);
  public final static native double Point_x_get(long jarg1, Point jarg1_);
  public final static native void Point_y_set(long jarg1, Point jarg1_, double jarg2);
  public final static native double Point_y_get(long jarg1, Point jarg1_);
  public final static native long new_Point();
  public final static native void delete_Point(long jarg1);
  public final static native long new_Guid();
  public final static native void delete_Guid(long jarg1);
  public final static native void Guid_SetNull(long jarg1, Guid jarg1_);
  public final static native void Guid_Create(long jarg1, Guid jarg1_);
  public final static native int Guid_FromString(long jarg1, Guid jarg1_, String jarg2);
  public final static native int Guid_ToString(long jarg1, Guid jarg1_, long jarg2);
  public final static native boolean Guid_equal(long jarg1, Guid jarg1_, long jarg2, Guid jarg2_);
  public final static native boolean Guid_notEqual(long jarg1, Guid jarg1_, long jarg2, Guid jarg2_);
  public final static native void Guid_data1_set(long jarg1, Guid jarg1_, long jarg2);
  public final static native long Guid_data1_get(long jarg1, Guid jarg1_);
  public final static native void Guid_data2_set(long jarg1, Guid jarg1_, int jarg2);
  public final static native int Guid_data2_get(long jarg1, Guid jarg1_);
  public final static native void Guid_data3_set(long jarg1, Guid jarg1_, int jarg2);
  public final static native int Guid_data3_get(long jarg1, Guid jarg1_);
  public final static native void Guid_data4_set(long jarg1, Guid jarg1_, short[] jarg2);
  public final static native short[] Guid_data4_get(long jarg1, Guid jarg1_);
  public final static native int GetErrorDescription(int jarg1, long jarg2);
  public final static native void GetErrorRecordCount(int[] jarg1);
  public final static native int GetErrorRecord(int jarg1, int[] jarg2, long jarg3);
  public final static native void ClearErrors();
  public final static native void SpatialReferenceInfo_auth_name_set(long jarg1, SpatialReferenceInfo jarg1_, String jarg2);
  public final static native String SpatialReferenceInfo_auth_name_get(long jarg1, SpatialReferenceInfo jarg1_);
  public final static native void SpatialReferenceInfo_auth_srid_set(long jarg1, SpatialReferenceInfo jarg1_, int jarg2);
  public final static native int SpatialReferenceInfo_auth_srid_get(long jarg1, SpatialReferenceInfo jarg1_);
  public final static native void SpatialReferenceInfo_srtext_set(long jarg1, SpatialReferenceInfo jarg1_, String jarg2);
  public final static native String SpatialReferenceInfo_srtext_get(long jarg1, SpatialReferenceInfo jarg1_);
  public final static native void SpatialReferenceInfo_srname_set(long jarg1, SpatialReferenceInfo jarg1_, String jarg2);
  public final static native String SpatialReferenceInfo_srname_get(long jarg1, SpatialReferenceInfo jarg1_);
  public final static native long new_SpatialReferenceInfo();
  public final static native void delete_SpatialReferenceInfo(long jarg1);
  public final static native long new_EnumSpatialReferenceInfo();
  public final static native void delete_EnumSpatialReferenceInfo(long jarg1);
  public final static native boolean EnumSpatialReferenceInfo_NextGeographicSpatialReference(long jarg1, EnumSpatialReferenceInfo jarg1_, long jarg2, SpatialReferenceInfo jarg2_);
  public final static native boolean EnumSpatialReferenceInfo_NextProjectedSpatialReference(long jarg1, EnumSpatialReferenceInfo jarg1_, long jarg2, SpatialReferenceInfo jarg2_);
  public final static native void EnumSpatialReferenceInfo_Reset(long jarg1, EnumSpatialReferenceInfo jarg1_);
  public final static native boolean FindSpatialReferenceBySRID(int jarg1, long jarg2, SpatialReferenceInfo jarg2_);
  public final static native boolean FindSpatialReferenceByName(String jarg1, long jarg2, SpatialReferenceInfo jarg2_);
  public final static native long new_Raster();
  public final static native void delete_Raster(long jarg1);
  public final static native String getErrorDescription(int jarg1);
  public final static native String getWstring(long jarg1);
  public final static native long getVectorWstring(long jarg1);
  public final static native String getString(long jarg1);
  public final static native long getVectorString(long jarg1);
  public final static native long SWIGPointShapeBufferUpcast(long jarg1);
  public final static native long SWIGMultiPointShapeBufferUpcast(long jarg1);
  public final static native long SWIGMultiPartShapeBufferUpcast(long jarg1);
  public final static native long SWIGMultiPatchShapeBufferUpcast(long jarg1);
}
