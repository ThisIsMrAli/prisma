package com.prisma.deploy.schema.types

import com.prisma.deploy.schema.SystemUserContext
import com.prisma.shared.models.{Field => _, _}
import com.prisma.shared.models
import sangria.schema
import sangria.schema.{Field, _}

import scala.reflect.ClassTag

object MigrationStepType {
  lazy val allTypes = List(
    Type,
    CreateModelType,
    DeleteModelType,
    UpdateModelType,
    CreateEnumType,
    DeleteEnumType,
    UpdateEnumType,
    CreateFieldType,
    UpdateFieldType,
    DeleteFieldType,
    CreateRelationType,
    UpdateRelationType,
    DeleteRelationType,
    UpdateSecretsType
  )

  case class MigrationStepAndSchema[T <: MigrationStep](step: T, schema: models.Schema)

  lazy val Type: InterfaceType[SystemUserContext, MigrationStepAndSchema[_]] = InterfaceType(
    "MigrationStep",
    "This is a migration step.",
    fields[SystemUserContext, MigrationStepAndSchema[_]](
      Field("type", StringType, resolve = _.value.step.getClass.getSimpleName)
    )
  )

  lazy val CreateModelType = fieldsHelper[CreateModel](
    Field("name", StringType, resolve = _.value.step.name)
  )

  lazy val DeleteModelType = fieldsHelper[DeleteModel](
    Field("name", StringType, resolve = _.value.step.name)
  )

  lazy val UpdateModelType = fieldsHelper[UpdateModel](
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", StringType, resolve = _.value.step.newName),
    Field("isEmbedded", OptionType(BooleanType), resolve = ctx => ctx.value.schema.getModelByName_!(ctx.value.step.newName).isEmbedded)
  )

  lazy val CreateEnumType = fieldsHelper[CreateEnum](
    Field("name", StringType, resolve = _.value.step.name),
    Field("values", ListType(StringType), resolve = ctx => ctx.value.schema.getEnumByName_!(ctx.value.step.name).values)
  )

  lazy val DeleteEnumType = fieldsHelper[DeleteEnum](
    Field("name", StringType, resolve = _.value.step.name)
  )

  lazy val UpdateEnumType = fieldsHelper[UpdateEnum](
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", OptionType(StringType), resolve = _.value.step.newName),
    Field("values", OptionType(ListType(StringType)), resolve = ctx => ctx.value.schema.getEnumByName_!(ctx.value.step.finalName).values)
  )

  lazy val CreateFieldType = fieldsHelper[CreateField](
    Field("model", StringType, resolve = _.value.step.model),
    Field("name", StringType, resolve = _.value.step.name),
    Field("typeName", StringType, resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).typeIdentifier.code),
    Field("isRequired", BooleanType, resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).isRequired),
    Field("isList", BooleanType, resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).isList),
    Field("unique", BooleanType, resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).isUnique),
    Field("enum", OptionType(StringType), resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).enum.map(_.name)),
    Field(
      "default",
      OptionType(StringType),
      resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.model, ctx.value.step.name).defaultValue.toString
    ),
    Field(
      "relation",
      OptionType(StringType),
      resolve = { ctx =>
        val (modelName, fieldName) = (ctx.value.step.model, ctx.value.step.name)
        ctx.value.schema.getFieldByName_!(modelName, fieldName).relationOpt.map(_.name)
      }
    )
  )

  lazy val DeleteFieldType = fieldsHelper[DeleteField](
    Field("model", StringType, resolve = _.value.step.model),
    Field("name", StringType, resolve = _.value.step.name)
  )

  // FIXME: all those options must just be populated if something actually changed
  lazy val UpdateFieldType = fieldsHelper[UpdateField](
    Field("model", StringType, resolve = _.value.step.model),
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", OptionType(StringType), resolve = _.value.step.newName),
    Field(
      "typeName",
      OptionType(StringType),
      resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.newModel, ctx.value.step.finalName).typeIdentifier.code
    ),
    Field(
      "isRequired",
      OptionType(BooleanType),
      resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.newModel, ctx.value.step.finalName).isRequired
    ),
    Field(
      "isList",
      OptionType(BooleanType),
      resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.newModel, ctx.value.step.finalName).isList
    ),
    Field(
      "unique",
      OptionType(BooleanType),
      resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.newModel, ctx.value.step.finalName).isUnique
    ),
    Field(
      "enum",
      OptionType(OptionType(StringType)),
      resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.newModel, ctx.value.step.finalName).enum.map(_.name)
    ),
    Field(
      "default",
      OptionType(OptionType(StringType)),
      resolve = ctx => ctx.value.schema.getFieldByName_!(ctx.value.step.newModel, ctx.value.step.finalName).defaultValue.map(_.toString)
    ),
    Field(
      "relation",
      OptionType(OptionType(StringType)),
      resolve = { ctx =>
        val (modelName, fieldName) = (ctx.value.step.newModel, ctx.value.step.finalName)
        ctx.value.schema.getFieldByName_!(modelName, fieldName).relationOpt.map(_.name)
      }
    )
  )

  lazy val CreateRelationType = fieldsHelper[CreateRelation](
    Field("name", StringType, resolve = _.value.step.name),
    Field("leftModel", StringType, resolve = ctx => ctx.value.schema.getRelationByName_!(ctx.value.step.name).modelAName),
    Field("rightModel", StringType, resolve = ctx => ctx.value.schema.getRelationByName_!(ctx.value.step.name).modelAName)
  )

  lazy val UpdateRelationType = fieldsHelper[UpdateRelation](
    Field("name", StringType, resolve = _.value.step.name),
    Field("newName", OptionType(StringType), resolve = _.value.step.newName)
  )

  lazy val DeleteRelationType = fieldsHelper[DeleteRelation](
    Field("name", StringType, resolve = _.value.step.name)
  )

  lazy val UpdateSecretsType = fieldsHelper[UpdateSecrets](
    Field("message", StringType, resolve = _ => "Secrets have been updated.")
  )

  def fieldsHelper[T <: MigrationStep](fields: schema.Field[SystemUserContext, MigrationStepAndSchema[T]]*)(implicit ct: ClassTag[T]) = {
    ObjectType(
      ct.runtimeClass.getSimpleName,
      "",
      interfaces[SystemUserContext, MigrationStepAndSchema[T]](Type),
      fields.toList
    )
  }
}
