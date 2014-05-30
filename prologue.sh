###############################################################################
# BRLTTY - A background process providing access to the console screen (when in
#          text mode) for a blind person using a refreshable braille display.
#
# Copyright (C) 1995-2014 by The BRLTTY Developers.
#
# BRLTTY comes with ABSOLUTELY NO WARRANTY.
#
# This is free software, placed under the terms of the
# GNU General Public License, as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any
# later version. Please see the file LICENSE-GPL for details.
#
# Web Page: http://mielke.cc/brltty/
#
# This software is maintained by Dave Mielke <dave@mielke.cc>.
###############################################################################

initialDirectory="$(pwd)"
programName="$(basename "${0}")"

programMessage() {
   local message="${1}"

   [ -z "${message}" ] || echo >&2 "${programName}: ${message}"
}

syntaxError() {
   local message="${1}"

   programMessage "${message}"
   exit 2
}

semanticError() {
   local message="${1}"

   programMessage "${message}"
   exit 3
}

internalError() {
   local message="${1}"

   programMessage "${message}"
   exit 4
}

getVariable() {
   local variable="${1}"

   eval 'echo "${'"${variable}"'}"'
}

setVariable() {
   local variable="${1}"
   local value="${2}"

   eval "${variable}"'="${value}"'
}

quoteString() {
return 0
   local string="${1}"

   local pattern="'"
   local replacement="'"'"'"'"'"'"'"
   string="${string//${pattern}/${replacement}}"
   echo "'${string}'"
}

verifyProgram() {
   local path="${1}"

   [ -e "${path}" ] || semanticError "program not found: ${path}"
   [ -f "${path}" ] || semanticError "not a file: ${path}"
   [ -x "${path}" ] || semanticError "not executable: ${path}"
}

testDirectory() {
   local path="${1}"

   [ -e "${path}" ] || return 1
   [ -d "${path}" ] || semanticError "not a directory: ${path}"
   return 0
}

verifyInputDirectory() {
   local path="${1}"

   testDirectory "${path}" || semanticError "directory not found: ${path}"
}

verifyOutputDirectory() {
   local path="${1}"

   if testDirectory "${path}"
   then
      [ -w "${path}" ] || semanticError "directory not writable: ${path}"
      rm -f -r -- "${path}/"*
   else
      mkdir -p "${path}"
   fi
}

resolveDirectory() {
   local path="${1}"

   (cd "${path}" && pwd)
}

needTemporaryDirectory() {
   cleanup() {
      set +e
      cd /
      [ -z "${temporaryDirectory}" ] || rm -f -r -- "${temporaryDirectory}"
   }
   trap "cleanup" 0

   umask 022
   [ -n "${TMPDIR}" -a -d "${TMPDIR}" -a -r "${TMPDIR}" -a -w "${TMPDIR}" -a -x "${TMPDIR}" ] || export TMPDIR="/tmp"
   temporaryDirectory="$(mktemp -d "${TMPDIR}/${programName}.XXXXXX")" && cd "${temporaryDirectory}" || exit "${?}"
}

programParameterCount=0
programParameterLabelWidth=0

addProgramParameter() {
   local label="${1}"
   local variable="${2}"
   local usage="${3}"

   setVariable "programParameterLabel_${programParameterCount}" "${label}"
   setVariable "programParameterVariable_${programParameterCount}" "${variable}"
   setVariable "programParameterUsage_${programParameterCount}" "${usage}"

   local length="${#label}"
   [ "${length}" -le "${programParameterLabelWidth}" ] || programParameterLabelWidth="${length}"

   setVariable "${variable}" ""
   programParameterCount=$((programParameterCount + 1))
}

programOptionLetters=""
programOptionString=""
programOptionOperandWidth=0

programOptionDefault_counter=0
programOptionDefault_flag=false
programOptionDefault_list=""
programOptionDefault_string=""

addProgramOption() {
   local letter="${1}"
   local type="${2}"
   local variable="${3}"
   local usage="${4}"

   [ "$(expr "${letter}" : '^[[:alnum:]]*$')" -eq 1 ] || internalError "invalid program option: ${letter}"
   [ -z "$(getVariable "programOptionType_${letter}")" ] || internalError "duplicate program option definition: -${letter}"

   local operand
   case "${type}"
   in
      flag | counter)
         operand=""
         ;;

      string.* | list.*)
         operand="${type#*.}"
         type="${type%%.*}"
         [ -n "${operand}" ] || internalError "missing program option operand type: -${letter}"
         ;;

      *) internalError "invalid program option type: ${type} (-${letter})";;
   esac

   setVariable "programOptionType_${letter}" "${type}"
   setVariable "programOptionVariable_${letter}" "${variable}"
   setVariable "programOptionOperand_${letter}" "${operand}"
   setVariable "programOptionUsage_${letter}" "${usage}"

   local default="$(getVariable "programOptionDefault_${type}")"
   setVariable "${variable}" "${default}"

   local length="${#operand}"
   [ "${length}" -le "${programOptionOperandWidth}" ] || programOptionOperandWidth="${length}"

   programOptionLetters="${programOptionLetters} ${letter}"
   programOptionString="${programOptionString}${letter}"
   [ "${length}" -eq 0 ] || programOptionString="${programOptionString}:"
}

addProgramUsageLine() {
   local line="${1}"

   setVariable "programUsageLine_${programUsageLineCount}" "${line}"
   programUsageLineCount=$((programUsageLineCount + 1))
}

addProgramUsageText() {
return 0
   local text="${1}"
   local width="${2}"
   local prefix="${3}"

   local indent="${#prefix}"
   local length=$((width - indent))

   [ "${length}" -gt 0 ] || {
      addProgramUsageLine "${prefix}"
      prefix="${prefix:-length+1}"
      prefix="${prefix//?/ }"
      length=1
   }

   while [ "${#text}" -gt "${length}" ]
   do
      local head="${text:0:length+1}"
      head="${head% *}"

      [ "${#head}" -le "${length}" ] || {
         head="${text%% *}"
         [ "${head}" != "${text}" ] || break
      }

      addProgramUsageLine "${prefix}${head}"
      text="${text:${#head}+1}"
      prefix="${prefix//?/ }"
   done

   addProgramUsageLine "${prefix}${text}"
}

showProgramUsageSummary() {
   programUsageLineCount=0
   set ${programOptionLetters}
   local width="${COLUMNS:-72}"

   local line="Usage: ${programName}"
   [ "${#}" -eq 0 ] || line="${line} [-option ...]"

   local index=0
   while [ "${index}" -lt "${programParameterCount}" ]
   do
      line="${line} $(getVariable "programParameterLabel_${index}")"
      index=$((index + 1))
   done

   addProgramUsageLine "${line}"

   [ "${programParameterCount}" -eq 0 ] || {
      addProgramUsageLine "Parameters:"

      local indent=$((programParameterLabelWidth + 2))
      local index=0

      while [ "${index}" -lt "${programParameterCount}" ]
      do
         local line="$(getVariable "programParameterLabel_${index}")"

         while [ "${#line}" -lt "${indent}" ]
         do
            line="${line} "
         done

         addProgramUsageText "$(getVariable "programParameterUsage_${index}")" "${width}" "  ${line}"
         index=$((index + 1))
      done
   }

   [ "${#}" -eq 0 ] || {
      addProgramUsageLine "Options:"

      local indent=$((3 + programOptionOperandWidth + 2))
      local letter

      for letter
      do
         local line="-${letter} $(getVariable "programOptionOperand_${letter}")"

         while [ "${#line}" -lt "${indent}" ]
         do
            line="${line} "
         done

         addProgramUsageText "$(getVariable "programOptionUsage_${letter}")" "${width}" "  ${line}"
      done
   }

   local index=0
   while [ "${index}" -lt "${programUsageLineCount}" ]
   do
      getVariable "programUsageLine_${index}"
      index=$((index + 1))
   done
}

addProgramOption h flag showProgramUsageSummary "show usage summary (this output), and then exit"

parseProgramOptions() {
   local letter

   while getopts ":${programOptionString}" letter
   do
      case "${letter}"
      in
        \?) syntaxError "unrecognized option: -${OPTARG}";;
         :) syntaxError "missing operand: -${OPTARG}";;

         *) eval 'local variable="${programOptionVariable_'"${letter}"'}"'
            eval 'local type="${programOptionType_'"${letter}"'}"'

            case "${type}"
            in
               counter) let "${variable} += 1";;
               flag) setVariable "${variable}" true;;
               list) setVariable "${variable}" "$(getVariable "${variable}") $(quoteString "${OPTARG}")";;
               string) setVariable "${variable}" "${OPTARG}";;
               *) internalError "unimplemented program option type: ${type} (-${letter})";;
            esac
            ;;
      esac
   done
}

parseProgramOptions='
   parseProgramOptions "${@}"
   shift $((OPTIND - 1))

   if "${showProgramUsageSummary}"
   then
      showProgramUsageSummary
      exit 0
   fi

   programParameterIndex=0
   while [ "${programParameterIndex}" -lt "${programParameterCount}" ]
   do
      setVariable "$(getVariable "programParameterVariable_${programParameterIndex}")" "${1}"
      shift 1
      programParameterIndex=$((programParameterIndex + 1))
   done
   unset programParameterIndex

   [ "${#}" -eq 0 ] || syntaxError "too many parameters"
'

programDirectory="$(dirname "${0}")"
programDirectory="$(resolveDirectory "${programDirectory}")"
